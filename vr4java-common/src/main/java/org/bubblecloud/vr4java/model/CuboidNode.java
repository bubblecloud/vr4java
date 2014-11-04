package org.bubblecloud.vr4java.model;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by tlaukkan on 10/26/2014.
 */
@Entity
public class CuboidNode extends SceneNode {
    @Column(nullable = false)
    private float dimensionX;
    @Column(nullable = false)
    private float dimensionY;
    @Column(nullable = false)
    private float dimensionZ;
    @Column(nullable = false)
    private float shininess;
    @Column(nullable = false)
    private String texture;

    public CuboidNode() {
        super(NodeType.CUBOID);
    }

    public CuboidNode(float dimensionX, float dimensionY, float dimensionZ, float shininess, String texture) {
        super(NodeType.CUBOID);
        this.dimensionX = dimensionX;
        this.dimensionY = dimensionY;
        this.dimensionZ = dimensionZ;
        this.shininess = shininess;
        this.texture = texture;
    }

    @Override
    public SceneNode clone() {
        final CuboidNode node = new CuboidNode();
        node.copyFrom(this);
        node.dimensionX = dimensionX;
        node.dimensionY = dimensionY;
        node.dimensionZ = dimensionZ;
        node.shininess = shininess;
        node.texture = texture;
        return node;
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

    public float getDimensionX() {
        return dimensionX;
    }

    public void setDimensionX(float dimensionX) {
        this.dimensionX = dimensionX;
    }

    public float getDimensionY() {
        return dimensionY;
    }

    public void setDimensionY(float dimensionY) {
        this.dimensionY = dimensionY;
    }

    public float getDimensionZ() {
        return dimensionZ;
    }

    public void setDimensionZ(float dimensionZ) {
        this.dimensionZ = dimensionZ;
    }
}
