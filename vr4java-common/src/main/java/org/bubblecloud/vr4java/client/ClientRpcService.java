package org.bubblecloud.vr4java.client;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.api.SceneService;
import org.bubblecloud.vr4java.api.SceneServiceImpl;
import org.bubblecloud.vr4java.api.SceneServiceListener;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.rpc.MessageHandler;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class ClientRpcService implements SceneService, MessageHandler {

    private static final Logger LOGGER = Logger.getLogger(ClientRpcService.class.getName());

    private SceneService sceneService = new SceneServiceImpl();

    @Override
    public UUID addScene(String name, long x, long y, long z) {
        return sceneService.addScene(name, x, y, z);
    }

    @Override
    public Collection<SceneNode> getNodes(UUID sceneId) {
        return sceneService.getNodes(sceneId);
    }

    @Override
    public List<SceneNode> getNodes(UUID sceneId, List<UUID> ids) {
        return sceneService.getNodes(sceneId, ids);
    }

    @Override
    public void setNodesDynamic(UUID sceneId, final List<UUID> ids, final List<Integer> indexes) {
        sceneService.setNodesDynamic(sceneId, ids, indexes);
    }

    @Override
    public void setSceneStateSlug(UUID sceneId, byte[] state) {
        LOGGER.debug("Client received state slug.");
        sceneService.setSceneStateSlug(sceneId, state);
    }

    @Override
    public void removeScene(UUID sceneId) {
        sceneService.removeScene(sceneId);
    }

    @Override
    public void updateNodes(UUID sceneId, List<SceneNode> nodes) {
        sceneService.updateNodes(sceneId, nodes);
    }

    @Override
    public void setNodesStatic(UUID sceneId, List<UUID> ids) {
        sceneService.setNodesStatic(sceneId, ids);
    }

    @Override
    public void playNodeAudio(UUID sceneId, UUID nodeId, byte[] bytes) {
        sceneService.playNodeAudio(sceneId, nodeId, bytes);
    }

    @Override
    public void addSceneServiceListener(SceneServiceListener sceneServiceListener) {
        sceneService.addSceneServiceListener(sceneServiceListener);
    }

    @Override
    public void removeSceneServiceListener(SceneServiceListener sceneServiceListener) {
        sceneService.removeSceneServiceListener(sceneServiceListener);
    }

    @Override
    public Scene getScene(UUID sceneId) {
        return sceneService.getScene(sceneId);
    }

    @Override
    public void setNodesDynamic(UUID sceneId, List<UUID> ids) {
        sceneService.setNodesDynamic(sceneId, ids);
    }

    @Override
    public void removeNodes(UUID sceneId, List<UUID> ids) {
        sceneService.removeNodes(sceneId, ids);
    }

    @Override
    public void addNodes(UUID sceneId, List<SceneNode> nodes) {
        sceneService.addNodes(sceneId, nodes);
    }

    @Override
    public void addScene(Scene scene, Collection<SceneNode> nodes) {
        sceneService.addScene(scene, nodes);
    }

    @Override
    public Collection<Scene> getScenes() {
        return sceneService.getScenes();
    }

    @Override
    public byte[] getSceneStateSlug(UUID sceneId) {
        return sceneService.getSceneStateSlug(sceneId);
    }

    @Override
    public void onMessage(byte messageType, int messageId, byte[] buffer, int startIndex, int length) {

    }
}
