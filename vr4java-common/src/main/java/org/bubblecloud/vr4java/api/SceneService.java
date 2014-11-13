package org.bubblecloud.vr4java.api;

import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.rpc.RpcEvent;
import org.bubblecloud.vr4java.rpc.RpcMethod;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by tlaukkan on 9/21/14.
 */
public interface SceneService {

    public static byte TYPE_STATE_SLUG = 10;

    public void addSceneServiceListener(final SceneServiceListener sceneServiceListener);

    public void removeSceneServiceListener(final SceneServiceListener sceneServiceListener);

    public Scene getScene(final UUID sceneId);

    public UUID addScene(final String name, final long x, final long y, final long z);

    public void addScene(final Scene scene, final Collection<SceneNode> nodes);

    public void removeScene(final UUID sceneId);

    @RpcMethod
    public Collection<Scene> getScenes();

    @RpcMethod
    public byte[] getSceneStateSlug(final UUID sceneId);

    @RpcEvent
    public void setSceneStateSlug(final UUID sceneId, final byte[] state);

    @RpcMethod
    public Collection<SceneNode> getNodes(final UUID sceneId);

    @RpcMethod
    public SceneNode getNode(final UUID sceneId, final UUID ids);

    @RpcMethod
    public List<SceneNode> getNodes(final UUID sceneId, final List<UUID> ids);

    @RpcEvent
    public void addNodes(final UUID sceneId, final List<SceneNode> nodes);

    @RpcEvent
    public void updateNodes(final UUID sceneId, final List<SceneNode> nodes);

    @RpcEvent
    public void removeNodes(final UUID sceneId, final List<UUID> ids);

    @RpcEvent
    public void setNodesDynamic(final UUID sceneId, final List<UUID> ids);

    @RpcEvent
    public void setNodesDynamic(final UUID sceneId, final List<UUID> ids, final List<Integer> indexes);

    @RpcEvent
    public void setNodesStatic(final UUID sceneId, final List<UUID> ids);

    /**
     * Plays node audio. Bytes of length 0 signals end of stream.
     * @param sceneId
     * @param nodeId
     * @param bytes
     */
    @RpcEvent
    public void playNodeAudio(final UUID sceneId, final UUID nodeId, final byte[] bytes);
}
