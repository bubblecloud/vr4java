package org.bubblecloud.vr4java.server;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.bubblecloud.vr4java.api.SceneServiceListener;
import org.bubblecloud.vr4java.client.ClientNetworkController;
import org.bubblecloud.vr4java.model.CuboidNode;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.model.Scene;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.addons.sitekit.util.PropertiesUtil;
import org.vaadin.addons.sitekit.util.TestUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class ClientServerTest {
    private static final Logger LOGGER = Logger.getLogger(ClientServerTest.class.getName());
    private final ServerMain serverMain = new ServerMain();
    private ClientNetworkController clientNetworkController;

    @Before
    public void before() throws Exception {
        TestUtil.before("vr4java", "vr4java-test");
        serverMain.start();
    }

    @After
    public void after() throws Exception {
        serverMain.stop();
        TestUtil.after("vr4java", "vr4java-test");
    }

    private byte[] receivedNodeAudioBytes;

    @Test
    public void testNetwork() throws Exception {
        PropertiesUtil.setCategoryRedirection("vr4java-client", "vr4java-test");
        clientNetworkController = new ClientNetworkController();
        clientNetworkController.start(null);

        Thread.sleep(200);

        final Scene scene = clientNetworkController.getScenes().values().iterator().next();

        final CuboidNode node = new CuboidNode();
        node.setId(UUID.randomUUID());
        node.setName("Avatar" + node.getId().hashCode());
        node.setOwnerCertificateFingerprint(clientNetworkController.getFingerprint());
        node.setTexture("");
        node.setPersistent(false);

        final List<SceneNode> nodeList = new ArrayList<>();
        nodeList.add(node);
        final List<UUID> nodeIdList = Collections.singletonList(node.getId());

        clientNetworkController.addNodes(scene, nodeList);
        clientNetworkController.setNodesDynamic(scene, nodeIdList);

        node.setX(1);

        clientNetworkController.setSceneStateSlug(scene, nodeList);

        Thread.sleep(500);

        // Added default scene content.
        Assert.assertEquals(5, clientNetworkController.getClientService().getNodes(scene.getId()).size());

        clientNetworkController.addSceneServiceListener(new SceneServiceListener() {
            @Override
            public void onAddNodes(UUID sceneId, List<UUID> nodeIds) {

            }

            @Override
            public void onUpdateNodes(UUID sceneId, List<UUID> nodeIds) {

            }

            @Override
            public void onRemoveNodes(UUID sceneId, List<UUID> nodeIds) {

            }

            @Override
            public void onStateSlug(UUID sceneId, List<UUID> nodeIds) {

            }

            @Override
            public void onPlayNodeAudio(UUID sceneId, UUID nodeId, byte[] bytes) {
                receivedNodeAudioBytes = bytes;
                LOGGER.info("Test received audio bytes: " + bytes.length);
            }
        });

        clientNetworkController.playNodeAudio(scene.getId(), node.getId(), "test".getBytes());
        while (receivedNodeAudioBytes == null) {
            Thread.sleep(100);
        }
        Assert.assertEquals("test", new String(receivedNodeAudioBytes));

        clientNetworkController.stop();

    }

}
