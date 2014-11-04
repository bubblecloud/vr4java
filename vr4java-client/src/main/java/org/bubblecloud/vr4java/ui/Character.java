package org.bubblecloud.vr4java.ui;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.bubblecloud.vr4java.model.SceneNode;

/**
 * Created by tlaukkan on 9/27/2014.
 */
public class Character {
    private SceneNode sceneNode;
    private Spatial spatial;
    private CharacterAnimator characterAnimator;
    private BetterCharacterControl characterControl;

    public Character(SceneNode sceneNode, Spatial spatial, CharacterAnimator characterAnimator, BetterCharacterControl characterControl) {
        this.sceneNode = sceneNode;
        this.spatial = spatial;
        this.characterAnimator = characterAnimator;
        this.characterControl = characterControl;
    }

    public SceneNode getSceneNode() {
        return sceneNode;
    }

    public Spatial getSpatial() {
        return spatial;
    }

    public CharacterAnimator getCharacterAnimator() {
        return characterAnimator;
    }

    public BetterCharacterControl getCharacterControl() {
        return characterControl;
    }
}
