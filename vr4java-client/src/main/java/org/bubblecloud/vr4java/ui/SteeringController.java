package org.bubblecloud.vr4java.ui;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.*;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.model.NodeType;
import org.bubblecloud.vr4java.util.VrConstants;

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
                new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Strafe Right",
                new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Walk Forward",
                new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Walk Backward",
                new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump",
                new KeyTrigger(KeyInput.KEY_SPACE));

        inputManager.addListener(this, "Strafe Left", "Strafe Right");
        inputManager.addListener(this, "Walk Forward", "Walk Backward");
        inputManager.addListener(this, "Jump");

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
        if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT && evt.isReleased()) {
            final CollisionResults results = new CollisionResults();
            final Vector2f click2d = inputManager.getCursorPosition();
            final Vector3f click3d = sceneContext.getCamera().getWorldCoordinates(
                    new Vector2f(click2d.x, click2d.y), 0f).clone();
            final Vector3f dir = sceneContext.getCamera().getWorldCoordinates(
                    new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
            final Ray ray = new Ray(click3d, dir);
            sceneContext.getRootNode().collideWith(ray, results);

            if (results.size() > 0){
                // The closest collision point is what was truly hit:
                final CollisionResult closest = results.getClosestCollision();
                final float distance = closest.getDistance();
                final Vector3f location = closest.getContactPoint();
                final String name = closest.getGeometry().getName();
                LOGGER.info("Picked " + name + " at " + location + " and " + distance + " meters away.");

                final Spatial spatial = closest.getGeometry();
                sceneContext.getSceneController().selectEditNode(spatial);
            }

        }
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

        if (evt.getKeyCode() == KeyInput.KEY_ADD && evt.isReleased()) {
            sceneContext.getSceneController().addEditNode(NodeType.CUBOID);
        }
        if (evt.getKeyCode() == KeyInput.KEY_SUBTRACT && evt.isReleased()) {
            sceneContext.getSceneController().removeEditNode();
        }
        if (evt.getKeyCode() == KeyInput.KEY_RETURN && evt.isReleased()) {
            sceneContext.getSceneController().saveEditNode();
        }

        if (evt.getKeyCode() == KeyInput.KEY_NUMPAD8 && evt.isReleased()) {
            sceneContext.getSceneController().translateEditNode(new Vector3f(-VrConstants.GRID_STEP_TRANSLATION, 0, 0));
        }
        if (evt.getKeyCode() == KeyInput.KEY_NUMPAD2 && evt.isReleased()) {
            sceneContext.getSceneController().translateEditNode(new Vector3f(VrConstants.GRID_STEP_TRANSLATION, 0, 0));
        }
        if (evt.getKeyCode() == KeyInput.KEY_NUMPAD4 && evt.isReleased()) {
            sceneContext.getSceneController().translateEditNode(new Vector3f(0, 0, VrConstants.GRID_STEP_TRANSLATION));
        }
        if (evt.getKeyCode() == KeyInput.KEY_NUMPAD6 && evt.isReleased()) {
            sceneContext.getSceneController().translateEditNode(new Vector3f(0, 0, -VrConstants.GRID_STEP_TRANSLATION));
        }
        if (evt.getKeyCode() == KeyInput.KEY_NUMPAD1 && evt.isReleased()) {
            sceneContext.getSceneController().translateEditNode(new Vector3f(0, -VrConstants.GRID_STEP_TRANSLATION, 0));
        }
        if (evt.getKeyCode() == KeyInput.KEY_NUMPAD3 && evt.isReleased()) {
            sceneContext.getSceneController().translateEditNode(new Vector3f(0, VrConstants.GRID_STEP_TRANSLATION, 0));
        }
        if (evt.getKeyCode() == KeyInput.KEY_LEFT && evt.isReleased()) {
            sceneContext.getSceneController().rotateEditNode(new Quaternion().fromAngleAxis(VrConstants.GRID_STEP_ROTATION, new Vector3f(0, 1f, 0)));
        }
        if (evt.getKeyCode() == KeyInput.KEY_RIGHT && evt.isReleased()) {
            sceneContext.getSceneController().rotateEditNode(new Quaternion().fromAngleAxis(-VrConstants.GRID_STEP_ROTATION, new Vector3f(0, 1f, 0)));
        }
        if (evt.getKeyCode() == KeyInput.KEY_UP && evt.isReleased()) {
            sceneContext.getSceneController().rotateEditNode(new Quaternion().fromAngleAxis(VrConstants.GRID_STEP_ROTATION, new Vector3f(0, 0, 1f)));
        }
        if (evt.getKeyCode() == KeyInput.KEY_DOWN && evt.isReleased()) {
            sceneContext.getSceneController().rotateEditNode(new Quaternion().fromAngleAxis(-VrConstants.GRID_STEP_ROTATION, new Vector3f(0, 0, 1f)));
        }
        if (evt.getKeyCode() == KeyInput.KEY_PGDN && evt.isReleased()) {
            sceneContext.getSceneController().rotateEditNode(new Quaternion().fromAngleAxis(-VrConstants.GRID_STEP_ROTATION, new Vector3f(1f, 0, 0)));
        }
        if (evt.getKeyCode() == KeyInput.KEY_END && evt.isReleased()) {
            sceneContext.getSceneController().rotateEditNode(new Quaternion().fromAngleAxis(VrConstants.GRID_STEP_ROTATION, new Vector3f(1f, 0, 0)));
        }
        if (evt.getKeyCode() == KeyInput.KEY_HOME && evt.isReleased()) {
            sceneContext.getSceneController().resetEditNodeRotation();
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {

    }
}
