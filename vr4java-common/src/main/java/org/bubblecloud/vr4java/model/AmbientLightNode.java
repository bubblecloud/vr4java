package org.bubblecloud.vr4java.model;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by tlaukkan on 10/26/2014.
 */
@Entity
public class AmbientLightNode extends SceneNode {
    @Column(nullable = false)
    private float colorRed;
    @Column(nullable = false)
    private float colorGreen;
    @Column(nullable = false)
    private float colorBlue;
    @Column(nullable = false)
    private float colorAlpha;

    public AmbientLightNode() {
        super(NodeType.AMBIENT_LIGHT);
    }

    public AmbientLightNode(float colorRed, float colorGreen, float colorBlue, float colorAlpha) {
        super(NodeType.AMBIENT_LIGHT);
        this.colorRed = colorRed;
        this.colorGreen = colorGreen;
        this.colorBlue = colorBlue;
        this.colorAlpha = colorAlpha;
    }

    @Override
    public SceneNode clone() {
        final AmbientLightNode node = new AmbientLightNode();
        node.copyFrom(this);
        node.colorRed = colorRed;
        node.colorGreen = colorGreen;
        node.colorBlue = colorBlue;
        node.colorAlpha = colorAlpha;
        return node;
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
