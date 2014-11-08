package org.bubblecloud.vr4java.server;

import junit.framework.Assert;
import org.bubblecloud.vr4java.client.ClientNetworkController;
import org.bubblecloud.vr4java.model.CuboidNode;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.model.Scene;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.addons.sitekit.util.TestUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class ClientServerTest {

    private final ServerMain serverMain = new ServerMain();
    private final ClientNetworkController clientNetworkController = new ClientNetworkController();

    @Before
    public void before() throws Exception {
        TestUtil.before("vr4java", "vr4java-server");
        serverMain.start();
    }

    @After
    public void after() throws Exception {
        serverMain.stop();
        TestUtil.after("vr4java", "vr4java-server");
    }

    @Test
    public void testNetwork() throws Exception {
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

        Thread.sleep(1000);

        // Added default scene content.
        Assert.assertEquals(5, clientNetworkController.getClientService().getNodes(scene.getId()).size());

        clientNetworkController.stop();

    }

}
