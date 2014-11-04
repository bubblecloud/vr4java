package org.bubblecloud.vr4java.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.util.UUID;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class SceneModelTest {

    final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testScene() throws Exception {
        final SceneModel sceneOne = new SceneModel();

        final SceneNode parent = new CuboidNode();
        parent.setId(UUID.randomUUID());
        parent.setParentId(new UUID(0L, 0L));

        final SceneNode node = new CuboidNode();
        node.setId(UUID.randomUUID());
        node.setParentId(parent.getParentId());

        sceneOne.addNode(parent);
        sceneOne.addNode(node);

        final SceneModel sceneTwo = new SceneModel(sceneOne.cloneNodes());

        Assert.assertEquals(toJson(sceneOne.getNodes()), toJson(sceneTwo.getNodes()));

        parent.setX(1);
        node.setX(1);

        Assert.assertNotSame(toJson(sceneOne.getNodes()), toJson(sceneTwo.getNodes()));

        sceneTwo.setStateSlug(sceneOne.getStateSlug());

        Assert.assertNotSame(toJson(sceneOne.getNodes()), toJson(sceneTwo.getNodes()));

        sceneOne.setNodeDynamic(parent.getId());
        sceneOne.setNodeDynamic(node.getId());
        sceneTwo.setNodeDynamic(parent.getId(), parent.getIndex());
        sceneTwo.setNodeDynamic(node.getId(), node.getIndex());

        sceneTwo.setStateSlug(sceneOne.getStateSlug());

        Assert.assertEquals(toJson(sceneOne.getNodes()), toJson(sceneTwo.getNodes()));
    }

    private String toJson(final Object object) throws Exception {
        final StringWriter writer = new StringWriter();
        mapper.writeValue(writer, object);
        return writer.getBuffer().toString();
    }
}
