package org.bubblecloud.vr4java.ui;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.api.SceneServiceListener;
import org.bubblecloud.vr4java.client.ClientNetworkController;
import org.bubblecloud.vr4java.client.ClientRpcService;
import org.bubblecloud.vr4java.model.*;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.util.*;

/**
 * Created by tlaukkan on 9/26/2014.
 */
public class SceneController implements SceneServiceListener {

    private static final Logger LOGGER = Logger.getLogger(SceneController.class.getName());

    private final SceneContext sceneContext;
    private AssetManager assetManager;
    private PhysicsSpace physicsSpace;
    private SteeringController steeringController;
    private ClientNetworkController networkController;
    private final ClientRpcService clientRpcService;

    private com.jme3.scene.Node rootNode;
    private CharacterAnimator characterAnimator;

    private final Scene scene;
    private List<SceneNode> dynamicNodes = new ArrayList<>();

    private Map<UUID, AnimationController> animationControllers = new HashMap<>();
    private Map<UUID, Spatial> spatials = new HashMap<>();

    private HashSet<UUID> addedNodes = new HashSet<>();

    private HashSet<UUID> updatedNodes = new HashSet<>();

    private HashSet<UUID> removedNodes = new HashSet<>();

    private HashSet<UUID> sluggedNodes = new HashSet<>();

    public SceneController(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
        this.networkController = sceneContext.getClientNetworkController();
        scene = networkController.getScenes().values().iterator().next();

        clientRpcService = networkController.getClientService();
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

    public void update(final float tpf) {
        final SceneNode characterNode = sceneContext.getCharacter().getSceneNode();
        final Spatial characterSpatial = sceneContext.getCharacter().getSpatial();

        updateNodeTransformation(characterSpatial, characterNode);

        synchronized (addedNodes) {
            final List<SceneNode> nodes = clientRpcService.getNodes(scene.getId(), new ArrayList(addedNodes));
            addNodes(nodes);
            addedNodes.clear();
        }
        synchronized (updatedNodes) {
            final List<SceneNode> nodes = clientRpcService.getNodes(scene.getId(), new ArrayList(updatedNodes));
            updateNodes(nodes);
            updatedNodes.clear();
        }
        synchronized (sluggedNodes) {
            final List<SceneNode> nodes = clientRpcService.getNodes(scene.getId(), new ArrayList(sluggedNodes));
            slugNodes(nodes);
            sluggedNodes.clear();
        }
        synchronized (removedNodes) {
            removeNodes(removedNodes);
            removedNodes.clear();
        }

        final List<SceneNode> nodes = clientRpcService.getNodes(scene.getId(), new ArrayList(spatials.keySet()));
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
        LOGGER.debug("Scene " + sceneId + " on nodes updated: " + nodeIds);

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

        synchronized (sluggedNodes) {
            if (sceneId.equals(scene.getId())) {
                sluggedNodes.addAll(nodeIds);
            }
        }

        if (sceneId.equals(scene.getId())) {
            networkController.setSceneStateSlug(scene, dynamicNodes);
        }
    }

    @Override
    public void onPlayNodeAudio(UUID sceneId, UUID nodeId, byte[] bytes) {
        //LOGGER.info("Client received node audio: " + nodeId + " (" + bytes.length + ")");
        sceneContext.getAudioPlaybackController().playAudio(sceneId, nodeId, bytes);
    }

    public void updateSpatialTransformation(SceneNode node, Spatial spatial) {
        spatial.setLocalTranslation(new Vector3f(
                node.getX() / 1000f,
                node.getY() / 1000f,
                node.getZ() / 1000f
        ));
        spatial.setLocalRotation(new Quaternion(
                node.getAx() / 10000f,
                node.getAy() / 10000f,
                node.getAz() / 10000f,
                node.getAw() / 10000f
        ));
    }

    public void addNodes(List<SceneNode> nodes) {
        for (final SceneNode node : nodes) {
            for (SceneNode ownNode : dynamicNodes) {
                if (ownNode.getId().equals(node.getId())) {
                    LOGGER.debug("Updated node index: " + node.getId() + ":" + node.getIndex());
                    ownNode.setIndex(node.getIndex());
                }
            }
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
            spatials.remove(nodeId);
        }
    }

    public void slugNodes(List<SceneNode> nodes) {
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

    public void updateNodeTransformation(Spatial spatial, SceneNode node) {
        setNodeTransformation(node, spatial.getLocalTranslation(), spatial.getLocalRotation());
    }

    public void setNodeTransformation(SceneNode node, Vector3f translation, Quaternion quaternion) {
        node.setX((int) (translation.x * 1000));
        node.setY((int) (translation.y * 1000));
        node.setZ((int) (translation.z * 1000));
        node.setAx((int) (quaternion.getX() * 10000));
        node.setAy((int) (quaternion.getY() * 10000));
        node.setAz((int) (quaternion.getZ() * 10000));
        node.setAw((int) (quaternion.getW() * 10000));
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

                spatial.setLocalTranslation(
                        sceneNode.getInterpolatedTranslation().x,
                        sceneNode.getInterpolatedTranslation().y,
                        sceneNode.getInterpolatedTranslation().z);

                spatial.setLocalRotation(
                        new Quaternion(
                                sceneNode.getInterpolatedRotation().getX(),
                                sceneNode.getInterpolatedRotation().getY(),
                                sceneNode.getInterpolatedRotation().getZ(),
                                sceneNode.getInterpolatedRotation().getW()
                        )
                );

                final RigidBodyControl rigidBodyControl = spatial.getControl(RigidBodyControl.class);
                if (rigidBodyControl != null) {
                    rigidBodyControl.setPhysicsLocation(spatial.getWorldTranslation());
                    rigidBodyControl.setPhysicsRotation(spatial.getWorldRotation());
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

    public Character addCharacter() {
        final String name = "Character";
        final String modelName = "jme3-open-asset-pack-v1/character/human/male/ogre/male.scene";

        final Spatial model = assetManager.loadModel(modelName);
        final com.jme3.scene.Node characterNode = new com.jme3.scene.Node(name);
        rootNode.attachChild(characterNode);
        characterNode.attachChild(model);

        final BetterCharacterControl characterControl = new BetterCharacterControl(0.5f, 2.5f, 8f);
        characterNode.addControl(characterControl);
        physicsSpace.add(characterControl);

        final SceneNode sceneNode = new ModelNode(modelName);
        sceneNode.setScene(scene);
        sceneNode.setPersistent(false);
        sceneNode.setName(name);
        updateNodeTransformation(characterNode, sceneNode);
        addDynamicNode(sceneNode);

        characterAnimator = new CharacterAnimator(sceneNode, model, steeringController);

        return new Character(sceneNode, characterNode, characterAnimator, characterControl);
    }

    public void addDynamicNode(final SceneNode sceneNode) {
        sceneNode.setId(networkController.calculateGlobalId(sceneNode.getName()));
        final List<SceneNode> modifiedDynamicNodes = Collections.synchronizedList(new ArrayList<>(dynamicNodes));
        modifiedDynamicNodes.add(sceneNode);
        dynamicNodes = modifiedDynamicNodes;
        networkController.addNodes(scene, Collections.singletonList(sceneNode));
        networkController.setNodesDynamic(scene, Collections.singletonList(sceneNode.getId()));
    }

    public void removeDynamicNode(final SceneNode sceneNode) {
        final List<SceneNode> modifiedDynamicNodes = Collections.synchronizedList(new ArrayList<>(dynamicNodes));
        modifiedDynamicNodes.remove(sceneNode);
        dynamicNodes = modifiedDynamicNodes;
        networkController.removeNodes(scene, Collections.singletonList(sceneNode.getId()));
    }

    private SceneNode editedNode;

    public void addEditNode(final NodeType nodeType) {
        if (editedNode != null) {
            LOGGER.warn("Already editing new node.");
            return;
        }
        if (nodeType.equals(NodeType.CUBOID)) {
            editedNode = new CuboidNode(1f, 1f, 1f, 1.0f,
                    "jme3-open-asset-pack-v1/textures/rose_fisher_grassy_gradient.jpg");
            editedNode.setScene(scene);
            editedNode.setName(UUID.randomUUID().toString());
            final Vector3f characterLocation = sceneContext.getCharacter().getSpatial().getWorldTranslation();
            final Vector3f nodeLocation = characterLocation.add(
                    sceneContext.getCharacter().getCharacterControl().getViewDirection().normalize().mult(2f));
            editedNode.setTranslation(new org.bubblecloud.vecmath.Vector3f(
                    nodeLocation.getX(),
                    nodeLocation.getY(),
                    nodeLocation.getZ()
            ));
            editedNode.setRotation(new org.bubblecloud.vecmath.Quaternion());
            editedNode.setPersistent(false);
            addDynamicNode(editedNode);
        }
    }

    public void translateEditNode(final Vector3f translation) {
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }
        final org.bubblecloud.vecmath.Vector3f location = editedNode.getTranslation();
        editedNode.setTranslation(location.add(new org.bubblecloud.vecmath.Vector3f(translation.x, translation.y, translation.z)));
    }

    public void rotateEditNode(final Quaternion rotation_) {
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }
        final org.bubblecloud.vecmath.Quaternion orientation = editedNode.getRotation();
        final org.bubblecloud.vecmath.Quaternion rotation = new org.bubblecloud.vecmath.Quaternion(rotation_.getX(),
                rotation_.getY(), rotation_.getZ(), rotation_.getW());
        final org.bubblecloud.vecmath.Quaternion newOrientation = orientation.mult(rotation);
        editedNode.setRotation(newOrientation);
    }

    public void removeEditNode() {
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }
        removeDynamicNode(editedNode);
        editedNode = null;
    }

}
