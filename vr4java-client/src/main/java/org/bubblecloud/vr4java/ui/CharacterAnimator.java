package org.bubblecloud.vr4java.ui;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.bubblecloud.vr4java.model.SceneNode;

import java.util.List;

/**
 * Created by tlaukkan on 9/26/2014.
 */
public class CharacterAnimator {
    private static final  long IDLE_TIME_MILLIS = 10000;

    public static final String ANIMATION_REST = "Rest";
    public static final String ANIMATION_STAND = "Stand";
    public static final String ANIMATION_WALK = "WalkBaked";

    final SceneNode node;
    final Spatial model;
    final SteeringController steeringController;

    private AnimationController animationController;

    private long lastUserControlTimeMillis = 0;
    private final List<String> animationNames;

    public CharacterAnimator(SceneNode node, Spatial model, SteeringController steeringController) {
        this.node = node;
        this.model = model;
        this.steeringController = steeringController;
        animationController = new AnimationController(model);
        animationNames = animationController.getAnimationNames();
        animationController.animate(ANIMATION_REST, 1f, 0f, 0);
    }

    public void update(float tpf) {

        final Vector3f viewDirection = steeringController.getCharacterControl().getViewDirection().normalize();
        final Vector3f walkDirection = steeringController.getCharacterControl().getWalkDirection().normalize();
        if (walkDirection.length() > 0.01) {
            animate(ANIMATION_WALK, 2.0f, 0.25f);
            lastUserControlTimeMillis = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() -  lastUserControlTimeMillis > IDLE_TIME_MILLIS) {
                animate(ANIMATION_REST, 0.5f, 2.0f);
            } else {
                animate(ANIMATION_STAND, 1.0f, 0.5f);
            }
        }
        animationController.update(tpf);
    }

    private void animate(final String animation, final float speedMultiplier, final float blendTime) {
        node.setStateAnimationIndex(animationNames.indexOf(animation));
        node.setStateAnimationRate((int) (speedMultiplier * 600));
        if (!animation.equals(animationController.getAnimationName())) {
            animationController.animate(animation, speedMultiplier, blendTime, 0);
        }
    }

}
