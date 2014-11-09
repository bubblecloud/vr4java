package org.bubblecloud.vr4java.api;

import java.util.List;
import java.util.UUID;

/**
 * Created by tlaukkan on 10/19/2014.
 */
public interface SceneServiceListener {

    void onAddNodes(final UUID sceneId, final List<UUID> nodeIds);

    void onUpdateNodes(final UUID sceneId, final List<UUID> nodeIds);

    void onRemoveNodes(final UUID sceneId, final List<UUID> nodeIds);

    void onStateSlug(final UUID sceneId, final List<UUID> nodeIds);

    void onPlayNodeAudio(final UUID sceneId, final UUID nodeId, final byte[] bytes);
}
