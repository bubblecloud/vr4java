package org.bubblecloud.vr4java.ui;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import org.bubblecloud.vr4java.client.ClientNetworkController;

/**
 * Created by tlaukkan on 9/27/2014.
 */
public class SceneContext {

    private InputManager inputManager;
    private AssetManager assetManager;
    private Camera camera;
    private PhysicsSpace physicsSpace;
    private Node rootNode;

    private ClientNetworkController clientNetworkController;
    private SteeringController steeringController;
    private SceneController sceneController;
    private AudioRecordController audioRecordController;

    private Character character;

    public InputManager getInputManager() {
        return inputManager;
    }

    public void setInputManager(InputManager inputManager) {
        this.inputManager = inputManager;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    public void setPhysicsSpace(PhysicsSpace physicsSpace) {
        this.physicsSpace = physicsSpace;
    }

    public ClientNetworkController getClientNetworkController() {
        return clientNetworkController;
    }

    public void setClientNetworkController(ClientNetworkController clientNetworkController) {
        this.clientNetworkController = clientNetworkController;
    }

    public SteeringController getSteeringController() {
        return steeringController;
    }

    public void setSteeringController(SteeringController steeringController) {
        this.steeringController = steeringController;
    }

    public SceneController getSceneController() {
        return sceneController;
    }

    public void setSceneController(SceneController sceneController) {
        this.sceneController = sceneController;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void setRootNode(Node rootNode) {
        this.rootNode = rootNode;
    }

    public AudioRecordController getAudioRecordController() {
        return audioRecordController;
    }

    public void setAudioRecordController(AudioRecordController audioRecordController) {
        this.audioRecordController = audioRecordController;
    }
}
