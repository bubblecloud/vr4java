package org.bubblecloud.vr4java.server;

import org.apache.log4j.Logger;
import org.bubblecloud.vecmath.ColorRGBA;
import org.bubblecloud.vecmath.FastMath;
import org.bubblecloud.vecmath.Quaternion;
import org.bubblecloud.vecmath.Vector3f;
import org.bubblecloud.vr4java.dao.SceneDao;
import org.bubblecloud.vr4java.dao.SceneNodeDao;
import org.bubblecloud.vr4java.model.*;
import org.vaadin.addons.sitekit.cache.PrivilegeCache;
import org.vaadin.addons.sitekit.dao.UserDao;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.Group;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by tlaukkan on 10/31/2014.
 */
public class SceneRepository {

    private static final Logger LOGGER = Logger.getLogger(SceneRepository.class);
    public static final String PRIVILEGE_ADMINISTRATE = "administrate";
    private ServerContext serverContext;

    public SceneRepository(final ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    public void ensureSceneExists() {
        synchronized (SceneRepository.class) {
            final EntityManager entityManager = serverContext.getEntityManager();
            final Company company = serverContext.getCompany();
            final Group adminGroup = UserDao.getGroup(serverContext.getEntityManager(), serverContext.getCompany(),
                    "administrator");

            if (SceneDao.getScenes(entityManager, company).size() > 0) {
                return;
            }

            final Scene scene = new Scene();
            scene.setOwner(serverContext.getCompany());
            scene.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            scene.setName("default");
            scene.setX(100000);
            scene.setY(100000);
            scene.setZ(100000);

            SceneDao.addScene(entityManager, scene);

            UserDao.addGroupPrivilege(entityManager, adminGroup, PRIVILEGE_ADMINISTRATE, scene.getId().toString());

            final List<SceneNode> sceneNodes = new ArrayList<>();

            final ColorRGBA lightColor = ColorRGBA.LightGray;
            final SceneNode directionalLightNode = new DirectionalLightNode(-0.37352666f, -0.50444174f, -0.7784704f,
                    lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), lightColor.getAlpha());
            directionalLightNode.setScene(scene);
            directionalLightNode.setName("directional light");
            directionalLightNode.setOwner(serverContext.getCompany());
            directionalLightNode.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            sceneNodes.add(directionalLightNode);

            final SceneNode ambientLightNode = new AmbientLightNode(
                    lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), lightColor.getAlpha());
            ambientLightNode.setScene(scene);
            ambientLightNode.setName("ambient light");
            ambientLightNode.setOwner(serverContext.getCompany());
            ambientLightNode.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            sceneNodes.add(ambientLightNode);

            final SceneNode floorNode = new CuboidNode(50f, 0.25f, 50f, 1.0f,
                    "jme3-open-asset-pack-v1/textures/rose_fisher_collage3_noncommercia.jpg");
            floorNode.setScene(scene);
            floorNode.setName("floor");
            floorNode.setOwner(serverContext.getCompany());
            floorNode.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            floorNode.setTranslation(new Vector3f(0, -3, 0));
            floorNode.setRotation(new Quaternion());
            sceneNodes.add(floorNode);

            final SceneNode torusNode = new TorusNode(30, 30, 20, 40, 1.0f,
                    "jme3-open-asset-pack-v1/textures/rose_fisher_collage3_noncommercia.jpg");
            torusNode.setScene(scene);
            torusNode.setName("torus");
            torusNode.setOwner(serverContext.getCompany());
            torusNode.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            final Quaternion rotation = new Quaternion();
            rotation.fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0));
            torusNode.setTranslation(new Vector3f(0, -15, 0));
            torusNode.setRotation(rotation);
            sceneNodes.add(torusNode);

            SceneNodeDao.addSceneNodes(entityManager, sceneNodes);

            /*final List<String> sceneNodeIds = new ArrayList<>();
            for (final SceneNode sceneNode : sceneNodes) {
                sceneNodeIds.add(sceneNode.getId().toString());
            }
            UserDao.addGroupPrivileges(entityManager, adminGroup, "administrate", sceneNodeIds);*/
        }
    }

    public void saveNodes(final UUID sceneId, final List<SceneNode> sceneNodes) {
        int persistentNodeCount = 0;
        for (final SceneNode sceneNode : sceneNodes) {
            if (sceneNode.isPersistent()) {
                persistentNodeCount++;
            }
        }

        if (persistentNodeCount == 0) {
            return;
        }

        final Scene scene = SceneDao.getScene(serverContext.getEntityManager(), sceneId);
        if (scene == null) {
            LOGGER.warn("Scene not found in repository: " + sceneId);
        }

        final List<SceneNode> nodesToAdd = new ArrayList<SceneNode>();
        final List<SceneNode> nodesToUpdate = new ArrayList<SceneNode>();
        for (final SceneNode sceneNode : sceneNodes) {
            if (serverContext.getUser() != null) {
                if (!serverContext.getUserCertificateFingerprint().equals(sceneNode.getOwnerCertificateFingerprint())) {
                    throw new SecurityException("User: " + serverContext.getUser()
                            + " tried to save not owned object: " + sceneNode.getId());
                }
            }

            if (sceneNode.isPersistent()) {
                sceneNode.setScene(scene);
                sceneNode.setOwner(serverContext.getCompany());
                if (sceneNode.getCreated() != null) {
                    nodesToUpdate.add(serverContext.getEntityManager().merge(sceneNode));
                } else {
                    nodesToAdd.add(sceneNode);
                }
            }
        }

        if (serverContext.getUser() != null && !PrivilegeCache.hasPrivilege(serverContext.getEntityManager(),
                serverContext.getCompany(), serverContext.getUser(), serverContext.getGroups(),
                PRIVILEGE_ADMINISTRATE, sceneId.toString())
                && !serverContext.getRoles().contains("administrator")) {
            throw new SecurityException("User " + serverContext.getUser().getUserId() + " node save denied to scene: " + sceneId);
        }

        SceneNodeDao.addSceneNodes(serverContext.getEntityManager(), nodesToAdd);
        SceneNodeDao.updateSceneNodes(serverContext.getEntityManager(), nodesToUpdate);
    }

    public List<SceneNode> loadNodes(final Scene scene) {
        return SceneNodeDao.getSceneNodes(serverContext.getEntityManager(), scene);
    }

    public List<Scene> loadScenes() {
        return SceneDao.getScenes(serverContext.getEntityManager(), serverContext.getCompany());
    }

}
