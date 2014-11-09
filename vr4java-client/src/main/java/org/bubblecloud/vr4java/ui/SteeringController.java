package org.bubblecloud.vr4java.ui;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.*;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import org.apache.log4j.Logger;

/**
 * Created by tlaukkan on 9/24/2014.
 */
public class SteeringController implements ActionListener, RawInputListener {
    private static final Logger LOGGER = Logger.getLogger(SteeringController.class.getName());

    private final SceneContext sceneContext;
    private final InputManager inputManager;
    private final Camera camera;
    private Character character;
    private Spatial characterSpatial;
    private BetterCharacterControl characterControl;
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private Vector3f viewDirection = new Vector3f(0, 0, 0);
    boolean leftStrafe = false, rightStrafe = false, forward = false, backward = false,
            leftRotate = false, rightRotate = false;

    public SteeringController(final SceneContext sceneContext) {
        // Add a physics character to the world
        this.sceneContext = sceneContext;
        this.inputManager = sceneContext.getInputManager();
        this.camera = sceneContext.getCamera();
        inputManager.addRawInputListener(this);
        inputManager.addMapping("Strafe Left",
                new KeyTrigger(KeyInput.KEY_A),
                new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Strafe Right",
                new KeyTrigger(KeyInput.KEY_D),
                new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Walk Forward",
                new KeyTrigger(KeyInput.KEY_W),
                new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Walk Backward",
                new KeyTrigger(KeyInput.KEY_S),
                new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Jump",
                new KeyTrigger(KeyInput.KEY_SPACE),
                new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("Shoot",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "Strafe Left", "Strafe Right");
        inputManager.addListener(this, "Walk Forward", "Walk Backward");
        inputManager.addListener(this, "Jump", "Shoot");

    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(final Character character) {
        this.character = character;
        this.characterSpatial = character.getSpatial();

        this.characterControl = character.getCharacterControl();

        ChaseCamera chaseCam = new ChaseCamera(camera, characterSpatial, inputManager);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setDefaultDistance(10f);
        chaseCam.setLookAtOffset(Vector3f.UNIT_Y.mult(2));
    }

    public BetterCharacterControl getCharacterControl() {
        return characterControl;
    }

    public void update(float tpf) {
        final Vector3f camDir = camera.getDirection();
        final Vector3f camLeft = camera.getLeft();
        camDir.y = 0;
        camDir.normalize();
        camLeft.y = 0;
        camLeft.normalize();

        final float timeDelta = tpf;
        final float angularVelocity = 60f * ( (float) Math.PI / 180f); // degrees per second in radians
        final float speed = 2.0f; // meters per second

        final float deltaAngle = angularVelocity * timeDelta;
        final float distanceDelta = speed * timeDelta;

        viewDirection.set(camDir);
        walkDirection.set(0, 0, 0);
        if (leftStrafe) {
            walkDirection.addLocal(camLeft.mult(speed));
        } else if (rightStrafe) {
            walkDirection.addLocal(camLeft.mult(speed).negate());
        }
        if (leftRotate) {
            viewDirection.addLocal(camLeft.mult(deltaAngle));
        } else if (rightRotate) {
            viewDirection.addLocal(camLeft.mult(deltaAngle).negate());
        }
        if (forward) {
            walkDirection.addLocal(camDir.mult(speed));
        } else if (backward) {
            walkDirection.addLocal(camDir.mult(speed).negate());
        }
        characterControl.setWalkDirection(walkDirection);
        characterControl.setViewDirection(viewDirection);
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Strafe Left")) {
            if (value) {
                leftStrafe = true;
            } else {
                leftStrafe = false;
            }
        } else if (binding.equals("Strafe Right")) {
            if (value) {
                rightStrafe = true;
            } else {
                rightStrafe = false;
            }
        } else if (binding.equals("Walk Forward")) {
            if (value) {
                forward = true;
            } else {
                forward = false;
            }
        } else if (binding.equals("Walk Backward")) {
            if (value) {
                backward = true;
            } else {
                backward = false;
            }
        } else if (binding.equals("Jump")) {
            characterControl.jump();
        }
    }

    public void onTalkBegin() {
        //LOGGER.info("Talk begin.");
        sceneContext.getAudioRecordController().beginAudioRecord();
    }

    public void onTalkEnd() {
        //LOGGER.info("Talk end.");
        sceneContext.getAudioRecordController().endAudioRecord();
    }

    public boolean isLeftStrafe() {
        return leftStrafe;
    }

    public void setLeftStrafe(boolean leftStrafe) {
        this.leftStrafe = leftStrafe;
    }

    public boolean isRightStrafe() {
        return rightStrafe;
    }

    public void setRightStrafe(boolean rightStrafe) {
        this.rightStrafe = rightStrafe;
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public boolean isBackward() {
        return backward;
    }

    public void setBackward(boolean backward) {
        this.backward = backward;
    }

    @Override
    public void beginInput() {

    }

    @Override
    public void endInput() {

    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {

    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {

    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {

    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {

    }

    private boolean pressToTalkDown = false;

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (evt.getKeyCode() == KeyInput.KEY_LCONTROL) {
            if (evt.isPressed()) {
                if (!pressToTalkDown) {
                    pressToTalkDown = true;
                    onTalkBegin();
                }
            }
            if (evt.isReleased()) {
                pressToTalkDown = false;
                onTalkEnd();
            }
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {

    }
}
