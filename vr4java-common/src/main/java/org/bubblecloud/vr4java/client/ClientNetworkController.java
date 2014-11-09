package org.bubblecloud.vr4java.client;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.bubblecloud.vr4java.api.PackageService;
import org.bubblecloud.vr4java.api.SceneService;
import org.bubblecloud.vr4java.api.SceneServiceListener;
import org.bubblecloud.vr4java.model.SceneModel;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.rpc.RpcConstants;
import org.bubblecloud.vr4java.rpc.RpcProxyUtil;
import org.bubblecloud.vr4java.rpc.RpcWsClient;
import org.bubblecloud.vr4java.util.ZipUtil;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class ClientNetworkController {
    private static final Logger LOGGER = Logger.getLogger(ClientNetworkController.class.getName());

    private String rpcUrl =
            (PropertiesUtil.getProperty("vr4java-client", "server-tsl").equals("true") ? "wss://" : "ws://") +
            PropertiesUtil.getProperty("vr4java-client", "server-address") + ":" +
            PropertiesUtil.getProperty("vr4java-client", "server-port")
            + "/ws/rpc/";

    private String packageUrlPrefix =
            (PropertiesUtil.getProperty("vr4java-client", "server-tsl").equals("true") ? "https://" : "http://") +
            PropertiesUtil.getProperty("vr4java-client", "server-address") + ":" +
            PropertiesUtil.getProperty("vr4java-client", "server-port") + "/asset/";

    private String packageCachePath = PropertiesUtil.getProperty("vr4java-client", "package-cache-path");
    private RpcWsClient client;
    private ClientRpcService clientService;
    private SceneService serverSceneService;
    private PackageService serverPackageService;

    private Map<UUID, Scene> scenes = new HashMap<>();

    public ClientNetworkController() {
        final URI uri = URI.create(rpcUrl);
        clientService = new ClientRpcService();
        client = new RpcWsClient("test-echo-client", uri, clientService, clientService);
    }

    public UUID calculateGlobalId(final String identifier) {
        final String fingerprint = client.getSealer().getFingerprint();
        final String globalIdentifier = fingerprint + identifier;
        final byte[] signatureBytes = new byte[RpcConstants.SIGNATURE_LENGTH];
        try {
            client.getSealer().sign(globalIdentifier.getBytes(Charset.forName("UTF-8")), signatureBytes, 0, RpcConstants.SIGNATURE_LENGTH);
        } catch (final Exception e) {
            throw new RuntimeException("Error in calculating object ID.", e);
        }

        long msb = 0;
        long lsb = 0;
        for (int i=0; i<8; i++)
            msb = (msb << 8) | (signatureBytes[i] & 0xff);
        for (int i=8; i<16; i++)
            lsb = (lsb << 8) | (signatureBytes[i] & 0xff);

        return new UUID(msb, lsb);
    }

    public void start(final ClientNetworkStartupListener listener) throws Exception {
        if (listener != null) {
            listener.message("Connecting...");
        }

        client.start();

        serverSceneService = RpcProxyUtil.createClientProxy(SceneService.class.getClassLoader(),
                SceneService.class, client.getRpcEndpoint());
        serverPackageService = RpcProxyUtil.createClientProxy(SceneService.class.getClassLoader(),
                PackageService.class, client.getRpcEndpoint());

        if (listener != null) {
            listener.message("Loading scene...");
        }

        for (final Scene scene : serverSceneService.getScenes()) {
            final Collection<SceneNode> nodes = serverSceneService.getNodes(scene.getId());

            final List<UUID> dynamicNodeIds = new ArrayList<UUID>();
            for (final SceneNode node : nodes) {
                if (node.getIndex() > 0) {
                    dynamicNodeIds.add(node.getId());
                }
            }

            clientService.addScene(scene, nodes);

            clientService.setNodesDynamic(scene.getId(), dynamicNodeIds);

            scenes.put(scene.getId(), scene);
            LOGGER.info("Added server scene to client: " + scene.getName() + " (" + scene.getId() + ")");
        }

        int i = 0;
        for (final String packageId : serverPackageService.getPackageIds()) {
            i++;
            LOGGER.info("Server uses package '" + packageId + "'.");

            final String packageUrl = packageUrlPrefix + "/" + packageId;
            final String packageFilePath = packageCachePath + File.separator + packageId + ".zip";
            final String packageDirectoryPath = packageCachePath + File.separator + packageId;

            final File packageCacheFile = new File(packageCachePath);
            if (!packageCacheFile.exists()) {
                packageCacheFile.mkdir();
            }

            final File packageFile = new File(packageFilePath);
            final File packageDirectory = new File(packageDirectoryPath);

            if (packageDirectory.exists()) {
                LOGGER.info("Package '" + packageId + "' already cached.");
                continue;
            }

            LOGGER.info("Downloading '" + packageId + "' ...");
            if (listener != null) {
                listener.message("Downloading packet " + i);
            }

            final URLConnection connection = new URL(packageUrl).openConnection();
            final FileOutputStream fileOutputStream = new FileOutputStream(packageFile, false);

            IOUtils.copy(connection.getInputStream(), fileOutputStream);
            fileOutputStream.close();

            ZipUtil.unzip(packageFile, packageDirectory);

            LOGGER.info("Downloaded package '" + packageId + "' ...");
            if (listener != null) {
                listener.message("Downloaded packet " + i);
            }
        }
    }

    public void addSceneServiceListener(SceneServiceListener sceneServiceListener) {
        clientService.addSceneServiceListener(sceneServiceListener);
    }

    public void removeSceneServiceListener(SceneServiceListener sceneServiceListener) {
        clientService.removeSceneServiceListener(sceneServiceListener);
    }

    public void setSceneStateSlug(Scene scene, List<SceneNode> nodeList) {
        serverSceneService.setSceneStateSlug(scene.getId(), SceneModel.getStateSlug(nodeList));
    }

    public void setNodesDynamic(Scene scene, List<UUID> nodeIdList) {
        serverSceneService.setNodesDynamic(scene.getId(), nodeIdList);
    }

    public void addNodes(Scene scene, List<SceneNode> nodeList) {
        for (final SceneNode node : nodeList) {
            node.setOwnerCertificateFingerprint(getFingerprint());
        }
        serverSceneService.addNodes(scene.getId(), nodeList);
    }

    public void playNodeAudio(UUID sceneId, UUID nodeId, byte[] bytes) {
        LOGGER.debug("Client sending play node audio for scene: " + sceneId + " node: " + nodeId);
        serverSceneService.playNodeAudio(sceneId, nodeId, bytes);
    }

    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            LOGGER.error("Error stopping client network controller: " + e);
        }
    }

    public Map<UUID, Scene> getScenes() {
        return scenes;
    }

    public ClientRpcService getClientService() {
        return clientService;
    }

    public String getFingerprint() {
        return client.getSealer().getFingerprint();
    }

    public static void main(final String[] args) throws Exception {
        // Configure logging.
        DOMConfigurator.configure("./log4j.xml");

        final ClientNetworkController main = new ClientNetworkController();

        main.start(null);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (main != null) {
                        main.stop();
                    }
                } catch (final Throwable t) {
                    LOGGER.error("Error stopping VR server.", t);
                }
            }
        }));
    }
}
