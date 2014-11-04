package org.bubblecloud.vr4java.model;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by tlaukkan on 10/26/2014.
 */
@Entity
public class DirectionalLightNode extends SceneNode {
    @Column(nullable = false)
    private float directionX;
    @Column(nullable = false)
    private float directionY;
    @Column(nullable = false)
    private float directionZ;
    @Column(nullable = false)
    private float colorRed;
    @Column(nullable = false)
    private float colorGreen;
    @Column(nullable = false)
    private float colorBlue;
    @Column(nullable = false)
    private float colorAlpha;

    public DirectionalLightNode() {
        super(NodeType.DIRECTIONAL_LIGHT);
    }

    public DirectionalLightNode(float directionX, float directionY, float directionZ, float colorRed, float colorGreen, float colorBlue, float colorAlpha) {
        super(NodeType.DIRECTIONAL_LIGHT);
        this.directionX = directionX;
        this.directionY = directionY;
        this.directionZ = directionZ;
        this.colorRed = colorRed;
        this.colorGreen = colorGreen;
        this.colorBlue = colorBlue;
        this.colorAlpha = colorAlpha;
    }

    @Override
    public SceneNode clone() {
        final DirectionalLightNode node = new DirectionalLightNode();
        node.copyFrom(this);
        node.directionX = directionX;
        node.directionY = directionY;
        node.directionZ = directionZ;
        node.colorRed = colorRed;
        node.colorGreen = colorGreen;
        node.colorBlue = colorBlue;
        node.colorAlpha = colorAlpha;
        return node;
    }

    public float getDirectionX() {
        return directionX;
    }

    public void setDirectionX(float directionX) {
        this.directionX = directionX;
    }

    public float getDirectionY() {
        return directionY;
    }

    public void setDirectionY(float directionY) {
        this.directionY = directionY;
    }

    public float getDirectionZ() {
        return directionZ;
    }

    public void setDirectionZ(float directionZ) {
        this.directionZ = directionZ;
    }

    public float getColorRed() {
        return colorRed;
    }

    public void setColorRed(float colorRed) {
        this.colorRed = colorRed;
    }

    public float getColorGreen() {
        return colorGreen;
    }

    public void setColorGreen(float colorGreen) {
        this.colorGreen = colorGreen;
    }

    public float getColorBlue() {
        return colorBlue;
    }

    public void setColorBlue(float colorBlue) {
        this.colorBlue = colorBlue;
    }

    public float getColorAlpha() {
        return colorAlpha;
    }

    public void setColorAlpha(float colorAlpha) {
        this.colorAlpha = colorAlpha;
    }
}
