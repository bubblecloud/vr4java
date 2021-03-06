package org.bubblecloud.vr4java.ui;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.api.SceneService;
import org.bubblecloud.vr4java.client.ClientNetwork;
import org.bubblecloud.vr4java.model.CuboidNode;
import org.bubblecloud.vr4java.model.NodeType;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.util.C;
import org.bubblecloud.vr4java.util.VrConstants;

import java.util.*;

/**
 * Created by tlaukkan on 11/13/2014.
 */
public class EditController {

    private static final Logger LOGGER = Logger.getLogger(SceneController.class.getName());

    private SceneContext sceneContext;
    private SceneController sceneController;
    private ClientNetwork clientNetwork;
    private SceneService sceneService;
    private boolean editMode = false;

    public EditController(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
        this.sceneController = sceneContext.getSceneController();
        this.clientNetwork = sceneContext.getClientNetwork();
        this.sceneService = clientNetwork.getClientService().getSceneService();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void update(float tpf) {

    }

    public SceneNode getEditedNode() {
        return editedNode;
    }

    private SceneNode editedNode;

    public void addEditNode(final NodeType nodeType) {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }

        if (nodeType.equals(NodeType.CUBOID)) {
            if (editedNode != null) {
                final SceneNode newNode = editedNode.clone();
                saveEditNode();
                editedNode = newNode;
            } else {
                editedNode = new CuboidNode(1f, 1f, 1f, 1.0f,
                        "jme3-open-asset-pack-v1/textures/rose_fisher_grassy_gradient.jpg");
                final Vector3f characterLocation = sceneContext.getCharacter().getSpatial().getWorldTranslation();
                final Vector3f nodeLocation = characterLocation.add(
                        sceneContext.getCharacter().getCharacterControl().getViewDirection().normalize().mult(2f));
                editedNode.setTranslation(snapToGrid(C.c(nodeLocation)));
                editedNode.setRotation(new org.bubblecloud.vecmath.Quaternion());
            }

            editedNode.setScene(sceneController.getScene());
            editedNode.setName(UUID.randomUUID().toString());
            editedNode.setPersistent(false);
            sceneController.addControlledNode(editedNode);

            editedNode.setId(clientNetwork.calculateGlobalId(editedNode.getName()));
            clientNetwork.addNodes(sceneController.getScene(), Collections.singletonList(editedNode));
            clientNetwork.setNodesDynamic(sceneController.getScene(), Collections.singletonList(editedNode.getId()));
        }
    }

    public void moveEditNode(final Vector3f translation) {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }

        final org.bubblecloud.vecmath.Vector3f location = editedNode.getTranslation();
        editedNode.setTranslation(location.add(new org.bubblecloud.vecmath.Vector3f(translation.x, translation.y, translation.z)));
    }

    public void rotate(final Quaternion rotation_) {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }

        final org.bubblecloud.vecmath.Quaternion orientation = editedNode.getRotation();
        final org.bubblecloud.vecmath.Quaternion rotation = new org.bubblecloud.vecmath.Quaternion(rotation_.getX(),
                rotation_.getY(), rotation_.getZ(), rotation_.getW());
        final org.bubblecloud.vecmath.Quaternion newOrientation = rotation.mult(orientation);
        editedNode.setRotation(newOrientation);
    }

    public void moveAndSnapToGrid(final Vector3f translationDelta) {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }

        final org.bubblecloud.vecmath.Vector3f translation = C.c(translationDelta);
        final org.bubblecloud.vecmath.Vector3f location = editedNode.getTranslation().add(translation);
        setTranslationAndSnapToGrid(location);
    }

    public void setTranslationAndSnapToGrid(org.bubblecloud.vecmath.Vector3f translation) {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }

        snapToGrid(translation);
        editedNode.setTranslation(translation);
    }

    public void resetEditNodeRotation() {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }

        editedNode.setRotation(new org.bubblecloud.vecmath.Quaternion());
    }

    public void removeEditNode() {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }

        sceneController.removeControlledNode(editedNode);
        clientNetwork.setNodesStatic(sceneController.getScene(), Arrays.asList(editedNode.getId()));
        clientNetwork.removeNodes(sceneController.getScene(), Collections.singletonList(editedNode.getId()));

        editedNode = null;
    }

    public void saveEditNode() {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return;
        }
        if (editedNode == null) {
            LOGGER.warn("Not editing a node.");
            return;
        }

        editedNode.setPersistent(true);

        sceneController.removeControlledNode(editedNode);
        clientNetwork.setNodesStatic(sceneController.getScene(), Arrays.asList(editedNode.getId()));
        clientNetwork.updateNodes(sceneController.getScene(), Arrays.asList(editedNode));

        editedNode = null;
    }

    public boolean selectEditNode(final Spatial spatial) {
        if (!editMode) {
            LOGGER.warn("Not in edit mode.");
            return false;
        }
        if (editedNode != null) {
            saveEditNode();
        }

        final SceneNode node = sceneController.getNodeBySpatial(spatial);
        if (node != null) {
            editedNode = node.clone(); // Clone to detach this node from server slug updates.
            sceneController.addControlledNode(editedNode);
            clientNetwork.setNodesDynamic(sceneController.getScene(), Arrays.asList(editedNode.getId()));
            return true;
        } else {
            LOGGER.warn("Scene node not found for spatial: " + spatial.getName());
            return false;
        }
    }

    private org.bubblecloud.vecmath.Vector3f snapToGrid(org.bubblecloud.vecmath.Vector3f coordinate) {
        coordinate.x += Math.signum(coordinate.x) * VrConstants.GRID_STEP_TRANSLATION / 2;
        coordinate.y += Math.signum(coordinate.y) * VrConstants.GRID_STEP_TRANSLATION / 2;
        coordinate.z += Math.signum(coordinate.z) * VrConstants.GRID_STEP_TRANSLATION / 2;
        coordinate.x = coordinate.x - (coordinate.x) % VrConstants.GRID_STEP_TRANSLATION;
        coordinate.y = coordinate.y - (coordinate.y) % VrConstants.GRID_STEP_TRANSLATION;
        coordinate.z = coordinate.z - (coordinate.z) % VrConstants.GRID_STEP_TRANSLATION;
        return coordinate;
    }

}
