package org.bubblecloud.vr4java.ui;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import org.bubblecloud.vr4java.VrClient;
import org.bubblecloud.vr4java.client.ClientNetwork;

/**
 * Created by tlaukkan on 9/27/2014.
 */
public class SceneContext {

    private VrClient vrClient;
    private InputManager inputManager;
    private AssetManager assetManager;
    private Camera camera;
    private PhysicsSpace physicsSpace;
    private Node rootNode;
    private Node guiNode;

    private ClientNetwork clientNetwork;
    private SteeringController steeringController;
    private SceneController sceneController;
    private EditController editController;
    private SpeechSynthesiser speechSynthesiser;
    private Aide aide;
    private HudController hudController;

    private AudioRecordController audioRecordController;
    private AudioPlaybackController audioPlaybackController;

    public VrClient getVrClient() {
        return vrClient;
    }

    public void setVrClient(VrClient vrClient) {
        this.vrClient = vrClient;
    }

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

    public ClientNetwork getClientNetwork() {
        return clientNetwork;
    }

    public void setClientNetwork(ClientNetwork clientNetwork) {
        this.clientNetwork = clientNetwork;
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

    public EditController getEditController() {
        return editController;
    }

    public void setEditController(EditController editController) {
        this.editController = editController;
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

    public Node getGuiNode() {
        return guiNode;
    }

    public void setGuiNode(Node guiNode) {
        this.guiNode = guiNode;
    }

    public SpeechSynthesiser getSpeechSynthesiser() {
        return speechSynthesiser;
    }

    public void setSpeechSynthesiser(SpeechSynthesiser speechSynthesiser) {
        this.speechSynthesiser = speechSynthesiser;
    }

    public AudioRecordController getAudioRecordController() {
        return audioRecordController;
    }

    public void setAudioRecordController(AudioRecordController audioRecordController) {
        this.audioRecordController = audioRecordController;
    }

    public AudioPlaybackController getAudioPlaybackController() {
        return audioPlaybackController;
    }

    public void setAudioPlaybackController(AudioPlaybackController audioPlaybackController) {
        this.audioPlaybackController = audioPlaybackController;
    }

    public Aide getAide() {
        return aide;
    }

    public void setAide(Aide aide) {
        this.aide = aide;
    }

    public HudController getHudController() {
        return hudController;
    }

    public void setHudController(HudController hudController) {
        this.hudController = hudController;
    }
}
