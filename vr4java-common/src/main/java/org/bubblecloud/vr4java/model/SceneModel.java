package org.bubblecloud.vr4java.model;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.util.BytesUtil;

import java.util.*;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class SceneModel {
    private static final Logger LOGGER = Logger.getLogger(SceneModel.class.getName());

    private static final int MAX_DYNAMIC_NODE_COUNT = 1000;

    private SceneNode[] dynamicNodeTable = new SceneNode[MAX_DYNAMIC_NODE_COUNT];

    private Map<UUID, SceneNode> nodes = new TreeMap<UUID, SceneNode>();

    public SceneModel() {

    }

    public SceneModel(final Collection<SceneNode> nodes) {
        for (final SceneNode node : nodes) {

            addNode(node);

        }
    }

    public void addNode(final SceneNode node) {
        if (nodes.containsKey(node.getId())) {
            LOGGER.warn("Node already exist: " + node.getId());
            return;
        }
        node.setIndex(-1);
        nodes.put(node.getId(), node);
    }

    public void updateNode(final SceneNode node) {
        if (!nodes.containsKey(node.getId())) {
            LOGGER.warn("Updated node does not exist: " + node.getId());
            return;
        }
        nodes.get(node.getId()).update(node);
    }

    public void removeNode(final UUID id) {
        if (!nodes.containsKey(id)) {
            return;
        }
        final SceneNode node = nodes.get(id);
        if (node.getIndex() != -1) {
            setNodeStatic(node.getId());
        }
        nodes.remove(id);
    }

    public Collection<SceneNode> getNodes() {
        return nodes.values();
    }

    public SceneNode getNode(final UUID id) {
        return nodes.get(id);
    }

    public List<SceneNode> cloneNodes() {
        final List<SceneNode> clones = new ArrayList<>();
        for (final SceneNode original : nodes.values()) {
            clones.add(original.clone());
        }
        return clones;
    }

    public void setNodeDynamic(final UUID id) {
        if (!nodes.containsKey(id)) {
            LOGGER.warn("Node to be set dynamic does not exist: " + id);
            return;
        }
        final SceneNode node = nodes.get(id);
        if (node.getIndex() != -1) {
            LOGGER.warn(node.getId() + " already dynamic.");
            return;
        }
        for (int i = 1; i < dynamicNodeTable.length; i++) {
            if (dynamicNodeTable[i] == null) {
                dynamicNodeTable[i] = node;
                node.setIndex(i);
                return;
            }
        }
        LOGGER.warn("Dynamic node table is full.");
        return;
    }

    public void setNodeDynamic(final UUID id, int index) {
        if (!nodes.containsKey(id)) {
            LOGGER.warn("Node to be set dynamic does not exist: " + id);
            return;
        }
        final SceneNode node = nodes.get(id);
        if (node.getIndex() != -1) {
            LOGGER.warn(node.getId() + " already dynamic.");
            return;
        }
        node.setIndex(index);
        dynamicNodeTable[index] = node;
        return;
    }

    public void setNodeStatic(final UUID id) {
        if (!nodes.containsKey(id)) {
            LOGGER.warn("Node to be set static does not exist: " + id);
            return;
        }
        final SceneNode node = nodes.get(id);
        dynamicNodeTable[node.getIndex()] = null;
        node.setIndex(-1);
    }

    public byte[] getStateSlug() {
        final byte[] state = new byte[MAX_DYNAMIC_NODE_COUNT * 2 * 14];
        int startIndex = 0;
        for (int i = 0; i < MAX_DYNAMIC_NODE_COUNT; i++) {
            final SceneNode node = dynamicNodeTable[i];
            if (node == null) {
                continue;
            }
            node.writeState(state, startIndex);
            startIndex += SceneNode.NODE_STATE_LENGTH;
        }
        return state;
    }


    public static byte[] getStateSlug(final List<SceneNode> nodes) {
        final byte[] bytes = new byte[nodes.size() * SceneNode.NODE_STATE_LENGTH];
        for (int i = 0; i < nodes.size(); i++) {
            final int startIndex = i * SceneNode.NODE_STATE_LENGTH;
            final SceneNode node = nodes.get(i);
            node.writeState(bytes, startIndex);
        }
        return bytes;
    }

    public List<UUID> setStateSlug(final byte[] state) {
        final List<UUID> nodeIds = new ArrayList<>();
        for (int i = 0; i < state.length / SceneNode.NODE_STATE_LENGTH; i++) {
            int startIndex = i * SceneNode.NODE_STATE_LENGTH;
            int nodeIndex = BytesUtil.readShort(state, startIndex);
            if (nodeIndex == 0) {
                break;
            }
            if (nodeIndex == -1) {
                continue;
            }
            final short idHash = BytesUtil.readShort(state, startIndex + 2);
            final SceneNode node = dynamicNodeTable[nodeIndex];
            if (node == null) {
                LOGGER.warn("No node in dynamic node list at index: " + nodeIndex);
                continue;
            }
            if (((short) node.getId().hashCode()) != idHash) {
                LOGGER.warn("Node id hash in dynamic node list did not match that of state for node ID: " + node.getId());
                continue;
            }
            node.readState(state, startIndex);
            nodeIds.add(node.getId());
        }
        return nodeIds;
    }

    public Map<UUID, String> getStateSlugNodeIdAndOwnerFingerprint(final byte[] state) {
        final HashMap<UUID, String> nodeIdsAndOwnerFingerprints = new HashMap<UUID, String>();
        for (int i = 0; i < state.length / SceneNode.NODE_STATE_LENGTH; i++) {
            int startIndex = i * SceneNode.NODE_STATE_LENGTH;
            int nodeIndex = BytesUtil.readShort(state, startIndex);
            if (nodeIndex == 0) {
                break;
            }
            if (nodeIndex == -1) {
                continue;
            }
            final short idHash = BytesUtil.readShort(state, startIndex + 2);
            final SceneNode node = dynamicNodeTable[nodeIndex];
            if (node == null) {
                LOGGER.warn("No node in dynamic node list at index: " + nodeIndex);
                continue;
            }
            if (((short) node.getId().hashCode()) != idHash) {
                LOGGER.warn("Node id hash in dynamic node list did not match that of state for node ID: " + node.getId());
                continue;
            }
            nodeIdsAndOwnerFingerprints.put(node.getId(), node.getOwnerCertificateFingerprint());
        }
        return nodeIdsAndOwnerFingerprints;
    }
}
