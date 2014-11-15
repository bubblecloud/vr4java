package org.bubblecloud.vr4java.ui;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.api.SceneService;
import org.bubblecloud.vr4java.api.SceneServiceListener;
import org.bubblecloud.vr4java.client.ClientNetwork;
import org.bubblecloud.vr4java.model.*;
import org.bubblecloud.vr4java.util.C;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.util.*;
import java.util.List;

/**
 * Created by tlaukkan on 9/26/2014.
 */
public class SceneController implements SceneServiceListener {

    private static final Logger LOGGER = Logger.getLogger(SceneController.class.getName());

    private final SceneContext sceneContext;
    private AssetManager assetManager;
    private PhysicsSpace physicsSpace;
    private SteeringController steeringController;
    private ClientNetwork networkController;
    private final SceneService sceneService;

    private com.jme3.scene.Node rootNode;
    private CharacterAnimator characterAnimator;

    private final Scene scene;
    private List<SceneNode> controlledNodes = new ArrayList<>();

    private Map<UUID, AnimationController> animationControllers = new HashMap<>();
    private Map<UUID, Spatial> spatials = new HashMap<>();

    private HashSet<UUID> addedNodes = new HashSet<>();

    private HashSet<UUID> updatedNodes = new HashSet<>();

    private HashSet<UUID> removedNodes = new HashSet<>();

    private HashSet<UUID> slugUpdatedNodes = new HashSet<>();

    public SceneController(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
        this.networkController = sceneContext.getClientNetwork();
        scene = networkController.getScenes().values().iterator().next();

        sceneService = networkController.getClientService().getSceneService();
        this.assetManager = sceneContext.getAssetManager();
        this.physicsSpace = sceneContext.getPhysicsSpace();
        this.rootNode = sceneContext.getRootNode();
        this.steeringController = sceneContext.getSteeringController();

        final List<UUID> nodeIds = new ArrayList<>();
        for (final SceneNode node : networkController.getClientService().getNodes(scene.getId())) {
            nodeIds.add(node.getId());
        }
        onAddNodes(scene.getId(), nodeIds);
        networkController.addSceneServiceListener(this);

    }

    public Scene getScene() {
        return scene;
    }

    public Map<UUID, Spatial> getSpatials() {
        return spatials;
    }

    public SceneNode getNodeBySpatial(final Spatial spatial) {
        for (final Map.Entry<UUID, Spatial> spatialMapEntry : spatials.entrySet()) {
            if (spatialMapEntry.getValue().equals(spatial)) {
                final UUID nodeId = spatialMapEntry.getKey();
                return sceneService.getNode(scene.getId(), nodeId);
            }
        }
        return null;
    }

    public void update(final float tpf) {
        final SceneNode characterNode = sceneContext.getCharacter().getSceneNode();
        final Spatial characterSpatial = sceneContext.getCharacter().getSpatial();

        updateNodeTransformation(characterSpatial, characterNode);

        synchronized (addedNodes) {
            final List<SceneNode> nodes = sceneService.getNodes(scene.getId(), new ArrayList(addedNodes));
            addNodes(nodes);
            addedNodes.clear();
        }
        synchronized (updatedNodes) {
            final List<SceneNode> nodes = sceneService.getNodes(scene.getId(), new ArrayList(updatedNodes));
            updateNodes(nodes);
            updatedNodes.clear();
        }
        synchronized (slugUpdatedNodes) {
            final List<SceneNode> nodes = sceneService.getNodes(scene.getId(), new ArrayList(slugUpdatedNodes));
            slugUpdateNode(nodes);
            slugUpdatedNodes.clear();
        }
        synchronized (removedNodes) {
            removeNodes(removedNodes);
            removedNodes.clear();
        }

        final List<SceneNode> nodes = sceneService.getNodes(scene.getId(), new ArrayList(spatials.keySet()));
        interpolateNodes(tpf, nodes);
    }

    @Override
    public void onAddNodes(UUID sceneId, List<UUID> nodeIds) {
        LOGGER.info("Scene " + sceneId + " on nodes added: " + nodeIds);

        synchronized (addedNodes) {
            if (sceneId.equals(scene.getId())) {
                addedNodes.addAll(nodeIds);
            }
        }

    }

    @Override
    public void onUpdateNodes(UUID sceneId, List<UUID> nodeIds) {
        LOGGER.info("Scene " + sceneId + " on nodes updated: " + nodeIds);

        synchronized (updatedNodes) {
            if (sceneId.equals(scene.getId())) {
                updatedNodes.addAll(nodeIds);
            }
        }
    }

    @Override
    public void onRemoveNodes(UUID sceneId, List<UUID> nodeIds) {
        LOGGER.debug("Scene " + sceneId + " on nodes removed: " + nodeIds);

        synchronized (removedNodes) {
            if (sceneId.equals(scene.getId())) {
                removedNodes.addAll(nodeIds);
            }
        }
    }

    @Override
    public void onStateSlug(UUID sceneId, List<UUID> nodeIds) {
        //LOGGER.info("Scene " + sceneId + " on state slug for nodes: " + nodeIds);

        synchronized (slugUpdatedNodes) {
            if (sceneId.equals(scene.getId())) {
                slugUpdatedNodes.addAll(nodeIds);
            }
        }

        if (sceneId.equals(scene.getId())) {
            networkController.setSceneStateSlug(scene, controlledNodes);
        }
    }

    @Override
    public void onSetNodesDynamic(UUID sceneId, List<UUID> ids) {

    }

    @Override
    public void onSetNodesDynamic(UUID sceneId, List<UUID> ids, List<Integer> indexes) {
        for (final SceneNode dynamicNode : controlledNodes) {
            for (int i = 0; i < ids.size(); i++) {
                final UUID id = ids.get(i);
                if (dynamicNode.getId().equals(id)) {
                    dynamicNode.setIndex(indexes.get(i));
                }
            }
        }
    }

    @Override
    public void onSetNodesStatic(UUID sceneId, List<UUID> ids) {

    }

    @Override
    public void onPlayNodeAudio(UUID sceneId, UUID nodeId, byte[] bytes) {
        //LOGGER.info("Client received node audio: " + nodeId + " (" + bytes.length + ")");
        sceneContext.getAudioPlaybackController().playAudio(sceneId, nodeId, bytes);
    }

    public void addNodes(List<SceneNode> nodes) {
        for (final SceneNode node : nodes) {
            /*for (SceneNode ownNode : controlledNodes) {
                if (ownNode.getId().equals(node.getId())) {
                    LOGGER.debug("Updated node index: " + node.getId() + ":" + node.getIndex());
                    ownNode.setIndex(node.getIndex());
                }
            }*/
            if (sceneContext.getCharacter().getSceneNode().getId().equals(node.getId())) {
                continue;
            }

            if (node instanceof AmbientLightNode) {
                final Spatial spatial = newAmbientLight((AmbientLightNode) node);
                rootNode.attachChild(spatial);
                spatials.put(node.getId(), spatial);
            }

            if (node instanceof DirectionalLightNode) {
                final Spatial spatial = newDirectionalLight((DirectionalLightNode) node);
                rootNode.attachChild(spatial);
                spatials.put(node.getId(), spatial);
            }

            if (node instanceof CuboidNode) {
                final Spatial spatial = newCuboid((CuboidNode) node);
                rootNode.attachChild(spatial);
                spatials.put(node.getId(), spatial);
            }

            if (node instanceof TorusNode) {
                final Spatial spatial = newTorus((TorusNode) node);
                rootNode.attachChild(spatial);
                spatials.put(node.getId(), spatial);
            }

            if (node instanceof ModelNode) {
                final Spatial spatial = newModel((ModelNode) node);
                rootNode.attachChild(spatial);
                spatials.put(node.getId(), spatial);
            }
        }
    }

    public void updateNodes(List<SceneNode> nodes) {
        final HashSet<UUID> nodeIds = new HashSet<UUID>();
        for (final SceneNode node : nodes) {
            nodeIds.add(node.getId());
        }
        removeNodes(nodeIds);
        addNodes(nodes);
        for (final SceneNode node : nodes) {
            final Spatial spatial = spatials.get(node.getId());
            if (spatial == null) {
                continue;
            }
            updateSpatialTransformation(node, spatial);
        }
    }

    public void removeNodes(HashSet<UUID> nodeIds) {
        for (final UUID nodeId : nodeIds) {
            final Spatial spatial = spatials.get(nodeId);
            if (spatial != null) {
                rootNode.detachChild(spatial);
            }
            if (animationControllers.containsKey(nodeId)) {
                animationControllers.remove(nodeId);
            }
            physicsSpace.remove(spatial);
            spatials.remove(nodeId);
        }
    }

    public void slugUpdateNode(List<SceneNode> nodes) {
        for (final SceneNode node : nodes) {
            final Spatial spatial = spatials.get(node.getId());
            if (spatial == null) {
                continue;
            }
            node.updateInterpolateTarget();
            //LOGGER.info("Slugged " + node.getName() + ": " + node.getAx() + "," + node.getAy() + "," + node.getAz() + "," + node.getAw());
            if (animationControllers.containsKey(node.getId())) {
                animationControllers.get(node.getId()).animate(node.getStateAnimationIndex(), node.getStateAnimationRate() / 600f);
            }
        }
    }


    public void updateSpatialTransformation(SceneNode node, Spatial spatial) {
        spatial.setLocalTranslation(C.c(node.getTranslation()));
        spatial.setLocalRotation((C.c(node.getRotation())));
    }

    public void updateNodeTransformation(Spatial spatial, SceneNode node) {
        node.setTranslation(C.c(spatial.getLocalTranslation()));
        node.setRotation(C.c(spatial.getLocalRotation()));
    }

    public void interpolateNodes(float tpf, List<SceneNode> nodes) {
        for (final SceneNode sceneNode : nodes) {
            if (sceneNode.getIndex() <= 0) {
                continue;
            }

            final Spatial spatial = spatials.get(sceneNode.getId());
            if (spatial == null) {
                continue;
            }

            if (sceneNode.interpolate(tpf)) {
                spatial.setLocalTranslation(C.c(sceneNode.getInterpolatedTranslation()));
                spatial.setLocalRotation(C.c(sceneNode.getInterpolatedRotation()));

                final RigidBodyControl rigidBodyControl = spatial.getControl(RigidBodyControl.class);
                if (rigidBodyControl != null) {
                    rigidBodyControl.setPhysicsLocation(spatial.getWorldTranslation());
                    rigidBodyControl.setPhysicsRotation(spatial.getWorldRotation());
                }

                if (spatial instanceof  Geometry) {
                    final Material material = ((Geometry) spatial).getMaterial();
                    final MatParam ambientColorParam = material.getParam("Ambient");
                    final ColorRGBA ambientColor = (ColorRGBA) ambientColorParam.getValue();
                    final boolean highlighted = ambientColor.equals(ColorRGBA.White);
                    final SceneNode editedNode = sceneContext.getEditController().getEditedNode();
                    final boolean edited = editedNode != null && sceneNode.getId().equals(editedNode.getId());
                    if (edited && !highlighted) {
                        material.setColor("Ambient", ColorRGBA.White);
                    } else if (!edited && highlighted) {
                        material.setColor("Ambient", ColorRGBA.LightGray);
                    }
                }

            }

        }
    }

    public Spatial newCuboid(CuboidNode node) {
        final Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Ambient", ColorRGBA.LightGray);
        material.setColor("Diffuse", ColorRGBA.LightGray);
        material.setTexture("DiffuseMap", assetManager.loadTexture(node.getTexture()));
        material.setFloat("Shininess", node.getShininess());

        final Box mesh = new Box(node.getDimensionX(), node.getDimensionY(), node.getDimensionZ());

        final Spatial spatial = new Geometry(node.getName(), mesh);
        spatial.setMaterial(material);
        updateSpatialTransformation(node, spatial);

        final RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
        spatial.addControl(rigidBodyControl);
        physicsSpace.add(spatial);

        return spatial;
    }

    public Spatial newTorus(TorusNode node) {
        final Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Ambient", ColorRGBA.LightGray);
        material.setColor("Diffuse", ColorRGBA.LightGray);
        material.setTexture("DiffuseMap", assetManager.loadTexture(node.getTexture()));
        material.setFloat("Shininess", node.getShininess());

        final Torus mesh = new Torus(node.getCircleSamples(), node.getRadialSamples(),
                node.getInnerRadius(), node.getOuterRadius());

        final Spatial spatial = new Geometry(node.getName(), mesh);
        spatial.setMaterial(material);
        updateSpatialTransformation(node, spatial);

        final RigidBodyControl domeControl = new RigidBodyControl(0);
        spatial.addControl(domeControl);
        physicsSpace.add(spatial);

        return spatial;
    }

    public Spatial newModel(ModelNode node) {
        final Spatial spatial = assetManager.loadModel(node.getModel());
        updateSpatialTransformation(node, spatial);
        animationControllers.put(node.getId(), new AnimationController(spatial));
        return spatial;
    }

    public Spatial newDirectionalLight(DirectionalLightNode node) {
        final DirectionalLight light = new DirectionalLight();

        light.setDirection(new Vector3f(node.getDirectionX(), node.getDirectionY(), node.getDirectionZ()));
        light.setColor(ColorRGBA.LightGray);

        final Spatial spatial = new Node(node.getName());
        rootNode.addLight(light);
        updateSpatialTransformation(node, spatial);
        return spatial;
    }

    public Spatial newAmbientLight(AmbientLightNode node) {
        final AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.LightGray);

        final Spatial spatial = new Node(node.getName());
        rootNode.addLight(light);
        updateSpatialTransformation(node, spatial);
        return spatial;
    }

    public Character setupCharacter() {
        final String name = "Character";
        final String modelName = PropertiesUtil.getProperty("vr4java-client", "character-model");

        final Spatial characterModel = assetManager.loadModel(modelName);
        final com.jme3.scene.Node characterSpatial = new com.jme3.scene.Node(name);
        rootNode.attachChild(characterSpatial);
        characterSpatial.attachChild(characterModel);

        final BetterCharacterControl characterControl = new BetterCharacterControl(0.5f, 2.5f, 8f);
        characterSpatial.addControl(characterControl);
        physicsSpace.add(characterControl);

        final SceneNode characterNode = new ModelNode(modelName);
        characterNode.setScene(scene);
        characterNode.setPersistent(false);
        characterNode.setName(name);
        updateNodeTransformation(characterSpatial, characterNode);

        addControlledNode(characterNode);

        characterNode.setId(networkController.calculateGlobalId(characterNode.getName()));
        networkController.addNodes(scene, Collections.singletonList(characterNode));
        networkController.setNodesDynamic(scene, Collections.singletonList(characterNode.getId()));

        characterAnimator = new CharacterAnimator(characterNode, characterModel, steeringController);

        return new Character(characterNode, characterSpatial, characterAnimator, characterControl);
    }

    public void addControlledNode(final SceneNode sceneNode) {
        final List<SceneNode> modifiedControlledNodes = Collections.synchronizedList(new ArrayList<>(controlledNodes));
        modifiedControlledNodes.add(sceneNode);
        controlledNodes = modifiedControlledNodes;
    }

    public void removeControlledNode(final SceneNode sceneNode) {
        final List<SceneNode> modifiedControlledNodes = Collections.synchronizedList(new ArrayList<>(controlledNodes));
        for (final SceneNode candidate : new ArrayList<SceneNode>(modifiedControlledNodes)) {
            if (candidate.getId().equals(sceneNode.getId())) {
                modifiedControlledNodes.remove(candidate);
            }
        }
        controlledNodes = modifiedControlledNodes;
    }


    public void loadScene() {

        if ("true".equals(PropertiesUtil.getProperty("vr4java-client", "test-objects"))) {
            // Test balls
            final Material ballMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            ballMaterial.setTexture("DiffuseMap", assetManager.loadTexture("jme3-open-asset-pack-v1/textures/rose_fisher_grassy_gradient.jpg"));
            ballMaterial.setBoolean("UseMaterialColors", true);
            ballMaterial.setColor("Ambient", ColorRGBA.LightGray);
            ballMaterial.setColor("Diffuse", ColorRGBA.LightGray);
            ballMaterial.setFloat("Shininess", 1);

            float radius = 0.2f;
            int n = 4;
            for (int i = -n; i <= n; i++) {
                for (int j = -n; j <= n; j++) {
                    for (int k = -n; k <= n; k++) {
                        Sphere sphere = new Sphere(20, 20, radius);
                        Geometry ballGeometry = new Geometry("Soccer ball", sphere);
                        ballGeometry.setMaterial(ballMaterial);
                        ballGeometry.setLocalTranslation(i * 2.2f * radius, k * 2.2f * radius, j * 2.2f * radius);
                        ballGeometry.addControl(new RigidBodyControl(.001f));
                        ballGeometry.getControl(RigidBodyControl.class).setRestitution(0f);
                        rootNode.attachChild(ballGeometry);
                        physicsSpace.add(ballGeometry);
                    }
                }
            }
        }

    }

}
