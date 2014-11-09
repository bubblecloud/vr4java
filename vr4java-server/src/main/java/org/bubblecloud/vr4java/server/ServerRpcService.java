package org.bubblecloud.vr4java.server;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.api.PackageService;
import org.bubblecloud.vr4java.api.SceneService;
import org.bubblecloud.vr4java.api.SceneServiceImpl;
import org.bubblecloud.vr4java.api.SceneServiceListener;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.rpc.MessageHandler;
import org.bubblecloud.vr4java.rpc.RpcProxyUtil;
import org.bubblecloud.vr4java.rpc.RpcWsServerEndpoint;
import org.vaadin.addons.sitekit.cache.UserClientCertificateCache;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.dao.UserDao;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.Group;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.module.content.dao.ContentDao;
import org.vaadin.addons.sitekit.module.content.model.Asset;
import org.vaadin.addons.sitekit.site.DefaultSiteUI;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by tlaukkan on 9/21/14.
 */
@javax.websocket.server.ServerEndpoint(value="/rpc/")
public class ServerRpcService extends RpcWsServerEndpoint implements SceneService, PackageService, MessageHandler {

    private static final Logger LOGGER = Logger.getLogger(ServerRpcService.class.getName());

    private String remoteFingerprint = null;

    private static SceneServiceImpl serverSceneService = new SceneServiceImpl();

    private SceneService clientService;

    private static Set<ServerRpcService> services = new HashSet<ServerRpcService>();

    /*static {
        serverSceneService.addScene("default", 100000, 100000, 100000);
    }*/

    private SceneRepository sceneRepository;
    private ServerContext serverContext;

    public static SceneService getSceneService() {
        return serverSceneService;
    }

    @Override
    public String getIdentifier() {
        return PropertiesUtil.getProperty("vr4java-server", "server-certificate-self-sign-host-name");
    }

    @Override
    public Object getRpcHandler() {
        return this;
    }

    public SceneService getClientService() {
        return clientService;
    }

    public void setClientService(SceneService clientService) {
        this.clientService = clientService;
    }

    @Override
    public MessageHandler getMessageHandler() {
        return this;
    }

    @Override
    public void onConnect(final String remoteFingerprint, final X509Certificate remoteCertificate) {
        LOGGER.info("Client connect: " + remoteFingerprint + " (" + this.toString() + ")");
        for (final ServerRpcService service : services) {
            if (service.getRemoteFingerprint().equals(remoteFingerprint)) {
                LOGGER.warn("Disconnecting already connected " + remoteFingerprint);
                service.onDisconnect(service.getRemoteFingerprint(), null);
            }
        }
        this.remoteFingerprint = remoteFingerprint;
        clientService = RpcProxyUtil.createClientProxy(SceneService.class.getClassLoader(),
                SceneService.class, getRpcEndpoint());
        synchronized (this.getClass()) {
            final HashSet<ServerRpcService> modifiedServices = new HashSet<ServerRpcService>(services);
            modifiedServices.add(this);
            services = Collections.synchronizedSet(modifiedServices);
        }

        final URI uri =  getSession().getRequestURI();
        final EntityManagerFactory entityManagerFactory = DefaultSiteUI.getEntityManagerFactory();
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityManager auditEntityManager = entityManagerFactory.createEntityManager();
        final Company company = CompanyDao.getCompany(entityManager, "*");

        User user;
        if ((user = UserClientCertificateCache.getUserByCertificate(remoteCertificate, false)) == null) {
            user = addUser(entityManager, company, remoteFingerprint, remoteCertificate);
        }

        serverContext = ServerContext.buildRpcServerContext(entityManager, auditEntityManager,
                getSealer().getFingerprint(),
                remoteFingerprint,
                company, user, uri);

        sceneRepository = new SceneRepository(serverContext);
    }


    @Override
    public void onDisconnect(final String remoteFingerprint, final X509Certificate remoteCertificate) {
        LOGGER.info("Client disconnect: " + remoteFingerprint + " (" + this.toString() + ")");

        synchronized (this.getClass()) {
            final HashSet<ServerRpcService> modifiedServices = new HashSet<ServerRpcService>(services);
            modifiedServices.remove(this);
            services = Collections.synchronizedSet(modifiedServices);
        }

        for (final Scene scene : getScenes()) {
            final List<UUID> nodesToRemove = new ArrayList<UUID>();
            for (final SceneNode node : getNodes(scene.getId())) {
                if (!node.isPersistent() && remoteFingerprint.equals(node.getOwnerCertificateFingerprint())) {
                    nodesToRemove.add(node.getId());
                }
            }
            removeNodes(scene.getId(), nodesToRemove);
            LOGGER.info("Client non persistent nodes evicted: " + remoteFingerprint + " (" + nodesToRemove + ")");
        }

        try {
            getSession().close();
        } catch (final Exception e) {
            LOGGER.error("Error closing disconnected session.", e);
        }
    }

    @Override
    public void addSceneServiceListener(SceneServiceListener sceneServiceListener) {
        serverSceneService.addSceneServiceListener(sceneServiceListener);
    }

    @Override
    public void removeSceneServiceListener(SceneServiceListener sceneServiceListener) {
        serverSceneService.removeSceneServiceListener(sceneServiceListener);
    }

    @Override
    public UUID addScene(String name, long x, long y, long z) {
        return serverSceneService.addScene(name, x, y, z);
    }

    @Override
    public Collection<SceneNode> getNodes(UUID sceneId) {
        return serverSceneService.getNodes(sceneId);
    }

    @Override
    public List<SceneNode> getNodes(UUID sceneId, List<UUID> ids) {
        return serverSceneService.getNodes(sceneId, ids);
    }

    @Override
    public void setSceneStateSlug(UUID sceneId, byte[] state) {
        serverSceneService.setSceneStateSlug(sceneId, state);
    }

    @Override
    public void removeScene(UUID sceneId) {
        serverSceneService.removeScene(sceneId);
    }

    @Override
    public void updateNodes(UUID sceneId, List<SceneNode> nodes) {
        serverSceneService.updateNodes(sceneId, nodes);
        for (final ServerRpcService service : services) {
            service.getClientService().updateNodes(sceneId, nodes);
        }
    }

    @Override
    public void setNodesStatic(UUID sceneId, List<UUID> ids) {
        serverSceneService.setNodesStatic(sceneId, ids);
        for (final ServerRpcService service : services) {
            service.getClientService().setNodesStatic(sceneId, ids);
        }
    }

    @Override
    public Scene getScene(UUID sceneId) {
        return serverSceneService.getScene(sceneId);
    }

    @Override
    public void setNodesDynamic(UUID sceneId, List<UUID> ids) {
        serverSceneService.setNodesDynamic(sceneId, ids);
        final List<SceneNode> nodes = serverSceneService.getNodes(sceneId, ids);
        final List<Integer> indexes = new ArrayList<Integer>();
        for (final SceneNode node : nodes) {
            indexes.add(node.getIndex());
        }
        for (final ServerRpcService service : services) {
            service.getClientService().setNodesDynamic(sceneId, ids, indexes);
        }
    }

    @Override
    public void setNodesDynamic(UUID sceneId, final List<UUID> ids, final List<Integer> indexes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNodes(UUID sceneId, List<UUID> ids) {
        serverSceneService.removeNodes(sceneId, ids);
        for (final ServerRpcService service : services) {
            service.getClientService().removeNodes(sceneId, ids);
        }
    }

    @Override
    public void addNodes(UUID sceneId, List<SceneNode> nodes) {
        for (final SceneNode node : nodes) {
            if (!remoteFingerprint.equals(node.getOwnerCertificateFingerprint())) {
                LOGGER.warn("Attempt to add nodes without proper owner certificate fingerprint.");
                return;
            }
        }
        sceneRepository.saveNodes(sceneId, nodes);
        serverSceneService.addNodes(sceneId, nodes);
        for (final ServerRpcService service : services) {
            service.getClientService().addNodes(sceneId, nodes);
        }
    }

    @Override
    public void addScene(Scene scene, Collection<SceneNode> nodes) {
        serverSceneService.addScene(scene, nodes);
    }

    @Override
    public Collection<Scene> getScenes() {
        return serverSceneService.getScenes();
    }

    @Override
    public byte[] getSceneStateSlug(UUID sceneId) {
        return serverSceneService.getSceneStateSlug(sceneId);
    }

    @Override
    public void onMessage(byte messageType, int messageId, byte[] buffer, int startIndex, int length) {

    }

    public static void sendStateSlugToClients() {
        for (final Scene scene : serverSceneService.getScenes()) {
            final byte[] stateSlug =  serverSceneService.getSceneStateSlug(scene.getId());
            for (final ServerRpcService service : services) {
                try {
                    service.getClientService().setSceneStateSlug(scene.getId(), stateSlug);
                } catch (final Exception e) {
                    LOGGER.warn("Error sending state slug to client: " + service.getRemoteFingerprint());
                }
            }
        }
    }

    public String getRemoteFingerprint() {
        return remoteFingerprint;
    }

    public User addUser(final EntityManager entityManager, final Company company, String remoteFingerprint, X509Certificate remoteCertificate) {
        LOGGER.info("Adding user for client: " + remoteFingerprint);

        final String encodedRemoteCertifcate;
        try {
            encodedRemoteCertifcate = org.apache.commons.codec.binary.Base64.encodeBase64String(remoteCertificate.getEncoded());
        } catch (CertificateEncodingException e) {
            LOGGER.error("Error encoding TSL client certificate for finding user from database.");
            throw new SecurityException("Error encoding TSL client certificate for finding user from database.");
        }

        final String[] nameParts = remoteCertificate.getSubjectDN().getName().split(" ");

        final User user = new User();
        user.setOwner(company);
        user.setEmailAddress(remoteFingerprint);
        user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        user.setPhoneNumber("");
        user.setPasswordHash("");
        user.setCertificate(encodedRemoteCertifcate);
        user.setCreated(new Date());
        user.setModified(new Date());

        if (UserDao.getGroup(entityManager, company, "vruser") == null) {
            UserDao.addGroup(entityManager, new Group(company, "vruser", "Default VR user group."));
        }

        UserDao.addUser(entityManager, user, UserDao.getGroup(entityManager, company, "user"));
        UserDao.addGroupMember(entityManager, UserDao.getGroup(entityManager, company, "vruser"), user);

        return user;
    }

    @Override
    public List<String> getPackageIds() {
        final EntityManager entityManager = DefaultSiteUI.getEntityManagerFactory().createEntityManager();
        final Company company = serverContext.getCompany();

        final List<Asset> assets = ContentDao.getAssetsByMimeType(entityManager, company, "application/x-zip-compressed");

        final List<String> packageIds = new ArrayList<>();
        for (final Asset asset : assets) {
            packageIds.add(asset.getName());
        }

        return packageIds;
    }

    @Override
    public void playNodeAudio(UUID sceneId, UUID nodeId, byte[] bytes) {
        LOGGER.debug("Server broadcasting play node audio for scene: " + sceneId + " node: " + nodeId);
        final Scene scene = getScene(sceneId);
        if (scene == null) {
            LOGGER.warn("Attempt to play node audio at none existent scene: " + sceneId);
            return;
        }
        final List<SceneNode> nodes = getNodes(sceneId, Arrays.asList(nodeId));
        if (nodes.size() == 0) {
            LOGGER.warn("Attempt to play node audio from none existent node: " + nodeId);
            return;
        }
        if (!remoteFingerprint.equals(nodes.get(0).getOwnerCertificateFingerprint())) {
            LOGGER.warn("Attempt to play node audio without proper owner certificate fingerprint.");
            return;
        }
        for (final ServerRpcService service : services) {
            try {
                service.getClientService().playNodeAudio(sceneId, nodeId, bytes);
            } catch (final Exception e) {
                LOGGER.warn("Error sending state slug to client: " + service.getRemoteFingerprint());
            }
        }
    }
}
