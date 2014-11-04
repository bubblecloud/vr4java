package org.bubblecloud.vr4java.model;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by tlaukkan on 10/26/2014.
 */
@Entity
public class TorusNode extends SceneNode {
    @Column(nullable = false)
    private int circleSamples;
    @Column(nullable = false)
    private int radialSamples;
    @Column(nullable = false)
    private float innerRadius;
    @Column(nullable = false)
    private float outerRadius;
    @Column(nullable = false)
    private float shininess;
    @Column(nullable = false)
    private String texture;

    public TorusNode() {
        super(NodeType.TORUS);
    }

    public TorusNode(int circleSamples, int radialSamples, float innerRadius, float outerRadius, float shininess, String texture) {
        super(NodeType.TORUS);
        this.circleSamples = circleSamples;
        this.radialSamples = radialSamples;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.shininess = shininess;
        this.texture = texture;
    }

    @Override
    public SceneNode clone() {
        final TorusNode node = new TorusNode();
        node.copyFrom(this);
        node.circleSamples = circleSamples;
        node.radialSamples = radialSamples;
        node.innerRadius = innerRadius;
        node.outerRadius = outerRadius;
        node.shininess = shininess;
        node.texture = texture;
        return node;
    }

    public int getCircleSamples() {
        return circleSamples;
    }

    public void setCircleSamples(int circleSamples) {
        this.circleSamples = circleSamples;
    }

    public int getRadialSamples() {
        return radialSamples;
    }

    public void setRadialSamples(int radialSamples) {
        this.radialSamples = radialSamples;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public void setInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public void setOuterRadius(float outerRadius) {
        this.outerRadius = outerRadius;
    }

    public float getShininess() {
        return shininess;
    }

    public void setShininess(float shininess) {
        this.shininess = shininess;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }
}
