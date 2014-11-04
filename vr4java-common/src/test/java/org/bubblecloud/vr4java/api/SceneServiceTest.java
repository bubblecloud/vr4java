package org.bubblecloud.vr4java.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import org.bubblecloud.vr4java.model.CuboidNode;
import org.bubblecloud.vr4java.model.SceneModel;
import org.bubblecloud.vr4java.model.SceneNode;
import org.junit.Test;

import java.io.StringWriter;
import java.util.*;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class SceneServiceTest {

    final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testScene() throws Exception {
        final SceneService sceneOneService = new SceneServiceImpl();
        final SceneService sceneTwoService = new SceneServiceImpl();
        final UUID sceneId = sceneOneService.addScene("test-scene", 0, 0, 0);

        final SceneNode localParent = new CuboidNode();
        localParent.setId(UUID.randomUUID());
        localParent.setParentId(new UUID(0L, 0L));

        final SceneNode localChild = new CuboidNode();
        localChild.setId(UUID.randomUUID());
        localChild.setParentId(localParent.getParentId());

        final List<SceneNode> nodes = new ArrayList<SceneNode>();
        nodes.add(localParent);
        nodes.add(localChild);

        sceneOneService.addNodes(sceneId, nodes);

        final List<SceneNode> nodeClones = new ArrayList<SceneNode>();
        for (final SceneNode original : sceneOneService.getNodes(sceneId)) {
            nodeClones.add(original.clone());
        }

        sceneTwoService.addScene(sceneOneService.getScene(sceneId), nodeClones);

        Assert.assertEquals(toJson(sceneOneService.getNodes(sceneId)), toJson(sceneTwoService.getNodes(sceneId)));

        final SceneNode parent = sceneOneService.getNodes(sceneId, Collections.singletonList(localParent.getId())).get(0);

        parent.setX(1);

        Assert.assertNotSame(toJson(sceneOneService.getNodes(sceneId)), toJson(sceneTwoService.getNodes(sceneId)));

        sceneTwoService.setSceneStateSlug(sceneId, sceneOneService.getSceneStateSlug(sceneId));

        Assert.assertNotSame(toJson(sceneOneService.getNodes(sceneId)), toJson(sceneTwoService.getNodes(sceneId)));

        sceneOneService.setNodesDynamic(sceneId, Collections.singletonList(parent.getId()));

        sceneTwoService.setNodesDynamic(sceneId, Collections.singletonList(parent.getId()), Collections.singletonList(parent.getIndex()));

        sceneTwoService.setSceneStateSlug(sceneId, sceneOneService.getSceneStateSlug(sceneId));

        Assert.assertEquals(toJson(sceneOneService.getNodes(sceneId)), toJson(sceneTwoService.getNodes(sceneId)));

        parent.setX(1);

        Assert.assertNotSame(toJson(sceneOneService.getNodes(sceneId)), toJson(sceneTwoService.getNodes(sceneId)));

        sceneTwoService.setSceneStateSlug(sceneId, SceneModel.getStateSlug(Collections.singletonList(parent)));

        Assert.assertEquals(toJson(sceneOneService.getNodes(sceneId)), toJson(sceneTwoService.getNodes(sceneId)));
    }

    private String toJson(final Object object) throws Exception {
        final StringWriter writer = new StringWriter();
        mapper.writeValue(writer, object);
        return writer.getBuffer().toString();
    }
}
