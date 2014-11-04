package org.bubblecloud.vr4java.model;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by tlaukkan on 10/26/2014.
 */
@Entity
public class ModelNode extends SceneNode {
    @Column(nullable = false)
    private String model;

    public ModelNode() {
        super(NodeType.MODEL);
    }

    public ModelNode(String model) {
        super(NodeType.MODEL);
        this.model = model;
    }

    @Override
    public SceneNode clone() {
        final ModelNode node = new ModelNode();
        node.copyFrom(this);
        node.model = model;
        return node;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
