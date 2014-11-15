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
import java.util.*;

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
            scene.setId(UUID.randomUUID());
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
            directionalLightNode.setId(UUID.randomUUID());
            directionalLightNode.setScene(scene);
            directionalLightNode.setName("directional light");
            directionalLightNode.setOwner(serverContext.getCompany());
            directionalLightNode.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            sceneNodes.add(directionalLightNode);

            final SceneNode ambientLightNode = new AmbientLightNode(
                    lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), lightColor.getAlpha());
            ambientLightNode.setId(UUID.randomUUID());
            ambientLightNode.setScene(scene);
            ambientLightNode.setName("ambient light");
            ambientLightNode.setOwner(serverContext.getCompany());
            ambientLightNode.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            sceneNodes.add(ambientLightNode);

            final SceneNode floorNode = new CuboidNode(50f, 0.25f, 50f, 1.0f,
                    "jme3-open-asset-pack-v1/textures/rose_fisher_collage3_noncommercia.jpg");
            floorNode.setId(UUID.randomUUID());
            floorNode.setScene(scene);
            floorNode.setName("floor");
            floorNode.setOwner(serverContext.getCompany());
            floorNode.setOwnerCertificateFingerprint(serverContext.getServerCertificateFingerprint());
            floorNode.setTranslation(new Vector3f(0, -3, 0));
            floorNode.setRotation(new Quaternion());
            sceneNodes.add(floorNode);

            final SceneNode torusNode = new TorusNode(30, 30, 20, 40, 1.0f,
                    "jme3-open-asset-pack-v1/textures/rose_fisher_collage3_noncommercia.jpg");
            torusNode.setId(UUID.randomUUID());
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

        final Scene scene = SceneDao.getScene(serverContext.getEntityManager(), sceneId);
        if (scene == null) {
            LOGGER.warn("Scene not found in repository: " + sceneId);
        }

        int persistentNodeCount = 0;
        final List<SceneNode> nodesToAdd = new ArrayList<SceneNode>();
        final List<SceneNode> nodesToUpdate = new ArrayList<SceneNode>();
        for (final SceneNode sceneNode : sceneNodes) {

            if (serverContext.getUser() != null) {
                // Allow update only if user is owner of the object or has administrative rights.
                if (!serverContext.getUserCertificateFingerprint().equals(sceneNode.getOwnerCertificateFingerprint()
                    ) && !PrivilegeCache.hasPrivilege(serverContext.getEntityManager(),
                        serverContext.getCompany(), serverContext.getUser(), serverContext.getGroups(),
                        PRIVILEGE_ADMINISTRATE, sceneId.toString())
                        && !serverContext.getRoles().contains("administrator")) {
                    throw new SecurityException("User: " + serverContext.getUser()
                            + " tried to save not owned object and was not administrator nor" +
                            " had administrator privileges on the scene: " + sceneNode.getId());
                }
            }

            if (sceneNode.isPersistent()) {
                persistentNodeCount++;
            }

            if (sceneNode.isPersistent()) {
                sceneNode.setScene(scene);
                sceneNode.setOwner(serverContext.getCompany());
                final SceneNode savedNode = SceneNodeDao.getSceneNode(serverContext.getEntityManager(), sceneNode.getId());
                if (savedNode != null) {
                    serverContext.getEntityManager().detach(savedNode);
                    sceneNode.setCreated(savedNode.getCreated());
                    sceneNode.setModified(new Date());
                    final SceneNode mergedNode = serverContext.getEntityManager().merge(sceneNode);
                    mergedNode.setId(sceneNode.getId());
                    mergedNode.setParentId(sceneNode.getParentId());
                    nodesToUpdate.add(mergedNode);
                } else {
                    nodesToAdd.add(sceneNode);
                }
            }
        }

        // The following is carried out only if saving one or more persistent nodes.
        if (persistentNodeCount == 0) {
            return;
        }

        // Allow only if user is administrator or has administrative rights to the scene.
        if (serverContext.getUser() != null && !PrivilegeCache.hasPrivilege(serverContext.getEntityManager(),
                serverContext.getCompany(), serverContext.getUser(), serverContext.getGroups(),
                PRIVILEGE_ADMINISTRATE, sceneId.toString())
                && !serverContext.getRoles().contains("administrator")) {
            throw new SecurityException("User " + serverContext.getUser().getUserId() + " node save denied to scene: " + sceneId);
        }

        if (nodesToAdd.size() != 0) {
            SceneNodeDao.addSceneNodes(serverContext.getEntityManager(), nodesToAdd);
        }
        if (nodesToUpdate.size() != 0) {
            SceneNodeDao.updateSceneNodes(serverContext.getEntityManager(), nodesToUpdate);
        }
    }

    public void removeNodes(final UUID sceneId, final List<UUID> sceneNodeIds) {

        final Scene scene = SceneDao.getScene(serverContext.getEntityManager(), sceneId);
        if (scene == null) {
            LOGGER.warn("Scene not found in repository: " + sceneId);
        }

        final List<SceneNode> sceneNodesToRemove = new ArrayList<SceneNode>();
        for (final UUID sceneNodeId : sceneNodeIds) {
            final SceneNode sceneNode = SceneNodeDao.getSceneNode(serverContext.getEntityManager(), sceneNodeId);
            if (sceneNode == null) {
                continue;
            }

            if (serverContext.getUser() != null) {
                // Allow update only if user is owner of the object or has administrative rights.
                if (!serverContext.getUserCertificateFingerprint().equals(sceneNode.getOwnerCertificateFingerprint()
                ) && !PrivilegeCache.hasPrivilege(serverContext.getEntityManager(),
                        serverContext.getCompany(), serverContext.getUser(), serverContext.getGroups(),
                        PRIVILEGE_ADMINISTRATE, sceneId.toString())
                        && !serverContext.getRoles().contains("administrator")) {
                    throw new SecurityException("User: " + serverContext.getUser()
                            + " tried to save not owned object and was not administrator nor" +
                            " had administrator privileges on the scene: " + sceneNode.getId());
                }
            }

            sceneNodesToRemove.add(sceneNode);
        }

        // The following is carried out only if removing one or more persistent nodes.
        if (sceneNodesToRemove.size() == 0) {
            return;
        }

        if (serverContext.getUser() != null && !PrivilegeCache.hasPrivilege(serverContext.getEntityManager(),
                serverContext.getCompany(), serverContext.getUser(), serverContext.getGroups(),
                PRIVILEGE_ADMINISTRATE, sceneId.toString())
                && !serverContext.getRoles().contains("administrator")) {
            throw new SecurityException("User " + serverContext.getUser().getUserId() + " node save denied to scene: " + sceneId);
        }
        SceneNodeDao.removeSceneNodes(serverContext.getEntityManager(), sceneNodesToRemove);

    }

    public void privilegeCheckDynamicStateChange(final UUID sceneId, final List<UUID> nodeIds) {
             for (final UUID sceneNodeId : nodeIds) {
                final SceneNode sceneNode = SceneNodeDao.getSceneNode(serverContext.getEntityManager(), sceneNodeId);
                if (sceneNode == null) {
                    continue;
                }

            final String ownerFingerprint = sceneNode.getOwnerCertificateFingerprint();
            // Allow update only if user is owner of the object or has administrative rights.
            if (!serverContext.getUserCertificateFingerprint().equals(ownerFingerprint
            ) && !PrivilegeCache.hasPrivilege(serverContext.getEntityManager(),
                    serverContext.getCompany(), serverContext.getUser(), serverContext.getGroups(),
                    PRIVILEGE_ADMINISTRATE, sceneId.toString())
                    && !serverContext.getRoles().contains("administrator")) {
                throw new SecurityException("User: " + serverContext.getUser()
                        + " tried to save not owned object and was not administrator nor" +
                        " had administrator privileges on the scene: " + sceneNodeId);
            }
        }
    }


    public void privilegeCheckUpdate(final UUID sceneId, final Map<UUID, String> nodeIdsAndOwnerFingerprints) {
        for (final UUID sceneNodeId : nodeIdsAndOwnerFingerprints.keySet()) {
            final String ownerFingerprint = nodeIdsAndOwnerFingerprints.get(sceneNodeId);
            // Allow update only if user is owner of the object or has administrative rights.
            if (!serverContext.getUserCertificateFingerprint().equals(ownerFingerprint
            ) && !PrivilegeCache.hasPrivilege(serverContext.getEntityManager(),
                    serverContext.getCompany(), serverContext.getUser(), serverContext.getGroups(),
                    PRIVILEGE_ADMINISTRATE, sceneId.toString())
                    && !serverContext.getRoles().contains("administrator")) {
                throw new SecurityException("User: " + serverContext.getUser()
                        + " tried to save not owned object and was not administrator nor" +
                        " had administrator privileges on the scene: " + sceneNodeId);
            }
        }
    }

    public List<SceneNode> loadNodes(final Scene scene) {
        return SceneNodeDao.getSceneNodes(serverContext.getEntityManager(), scene);
    }

    public List<Scene> loadScenes() {
        return SceneDao.getScenes(serverContext.getEntityManager(), serverContext.getCompany());
    }

}
