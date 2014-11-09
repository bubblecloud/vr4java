package org.bubblecloud.vr4java.api;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.model.SceneModel;

import java.util.*;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class SceneServiceImpl implements SceneService {
    private static final Logger LOGGER = Logger.getLogger(SceneServiceImpl.class.getName());

    private final Map<UUID, Scene> scenes = Collections.synchronizedMap(new HashMap<UUID, Scene>());
    private final Map<UUID, SceneModel> models = Collections.synchronizedMap(new HashMap<UUID, SceneModel>());
    private final List<SceneServiceListener> sceneServiceListeners = new ArrayList<>();


    @Override
    public void addSceneServiceListener(final SceneServiceListener sceneServiceListener) {
        synchronized (sceneServiceListeners) {
            sceneServiceListeners.add(sceneServiceListener);
        }
    }

    @Override
    public void removeSceneServiceListener(final SceneServiceListener sceneServiceListener) {
        synchronized (sceneServiceListeners) {
            sceneServiceListeners.remove(sceneServiceListener);
        }
    }

    @Override
    public UUID addScene(String name, final long x, final long y, final long z) {
        final Scene scene = new Scene();
        scene.setId(UUID.randomUUID());
        scene.setName(name);
        scene.setX(x);
        scene.setY(y);
        scene.setZ(z);
        scenes.put(scene.getId(), scene);
        models.put(scene.getId(), new SceneModel());
        models.put(scene.getId(), new SceneModel());
        return scene.getId();
    }

    @Override
    public synchronized void addScene(Scene scene, final Collection<SceneNode> nodes) {
        if (scenes.containsKey(scene.getId())) {
            LOGGER.warn("Scene add failed due to scene already exists: " + scene.getId());
            return;
        }
        synchronized (scene) {
            scenes.put(scene.getId(), scene);
            models.put(scene.getId(), new SceneModel(nodes));
        }
        final List<UUID> nodeIds = new ArrayList<>();
        for (final SceneNode node : nodes) {
            nodeIds.add(node.getId());
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onAddNodes(scene.getId(), nodeIds);
            }
        }
    }

    @Override
    public void removeScene(UUID sceneId) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene removal failed due to scene does not exist: " + sceneId);
            return;
        }
        synchronized (scene) {
            scenes.remove(sceneId);
            models.remove(sceneId);
        }
    }

    @Override
    public Scene getScene(UUID sceneId) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene removal failed due to scene does not exist: " + sceneId);
            return null;
        }
        return scene;
    }

    @Override
    public Collection<Scene> getScenes() {
        return scenes.values();
    }

    @Override
    public byte[] getSceneStateSlug(UUID sceneId) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        synchronized (scene) {
            return models.get(sceneId).getStateSlug();
        }
    }

    @Override
    public void setSceneStateSlug(UUID sceneId, byte[] state) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            return;
        }
        final List<UUID> nodeIds;
        synchronized (scene) {
            nodeIds = models.get(sceneId).setStateSlug(state);
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onStateSlug(sceneId, nodeIds);
            }
        }
    }

    @Override
    public Collection<SceneNode> getNodes(UUID sceneId) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        synchronized (scene) {
            return models.get(sceneId).getNodes();
        }
    }

    @Override
    public List<SceneNode> getNodes(UUID sceneId, List<UUID> ids) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        final List<SceneNode> nodes = new ArrayList<SceneNode>();
        synchronized (scene) {
            final SceneModel model = models.get(sceneId);
            for (final UUID id : ids) {
                final SceneNode node = model.getNode(id);
                if (node != null) {
                    nodes.add(node);
                }
            }
            return nodes;
        }
    }

    @Override
    public void addNodes(UUID sceneId, List<SceneNode> nodes) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        final List<UUID> nodeIds = new ArrayList<>();
        synchronized (scene) {
            for (final SceneNode node : nodes) {
                nodeIds.add(node.getId());
                models.get(sceneId).addNode(node);
            }
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onAddNodes(sceneId, nodeIds);
            }
        }
    }

    @Override
    public void updateNodes(UUID sceneId, List<SceneNode> nodes) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        final List<UUID> nodeIds = new ArrayList<>();
        synchronized (scene) {
            for (final SceneNode node : nodes) {
                nodeIds.add(node.getId());
                models.get(sceneId).updateNode(node);
            }
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onUpdateNodes(sceneId, nodeIds);
            }
        }
    }

    @Override
    public void removeNodes(UUID sceneId, List<UUID> ids) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        synchronized (scene) {
            for (final UUID id : ids) {
                models.get(sceneId).removeNode(id);
            }
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onRemoveNodes(sceneId, ids);
            }
        }
    }

    @Override
    public void setNodesDynamic(UUID sceneId, List<UUID> ids) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        synchronized (scene) {
            for (final UUID id : ids) {
                models.get(sceneId).setNodeDynamic(id);
            }
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onUpdateNodes(sceneId, ids);
            }
        }
    }

    @Override
    public void setNodesDynamic(UUID sceneId, final List<UUID> ids, final List<Integer> indexes) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        synchronized (scene) {
            for (int i = 0; i < ids.size(); i++) {
                final UUID id = ids.get(i);
                final int index = indexes.get(i);
                models.get(sceneId).setNodeDynamic(id, index);
            }
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onUpdateNodes(sceneId, ids);
            }
        }
    }

    @Override
    public void setNodesStatic(UUID sceneId, List<UUID> ids) {
        final Scene scene = scenes.get(sceneId);
        if (scene == null) {
            LOGGER.warn("Scene does not exist: " + sceneId);
            throw new IllegalArgumentException("Scene does not exist: " + sceneId);
        }
        synchronized (scene) {
            for (final UUID id : ids) {
                models.get(sceneId).setNodeStatic(id);
            }
        }
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onUpdateNodes(sceneId, ids);
            }
        }
    }


    @Override
    public void playNodeAudio(UUID sceneId, UUID nodeId, byte[] bytes) {
        synchronized (sceneServiceListeners) {
            for (final SceneServiceListener listener : sceneServiceListeners) {
                listener.onPlayNodeAudio(sceneId, nodeId, bytes);
            }
        }
    }
}
