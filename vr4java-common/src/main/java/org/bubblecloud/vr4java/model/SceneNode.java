package org.bubblecloud.vr4java.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bubblecloud.vecmath.Quaternion;
import org.bubblecloud.vecmath.Vector3f;
import org.bubblecloud.vr4java.util.BytesUtil;
import org.bubblecloud.vr4java.util.VrConstants;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * Created by tlaukkan on 9/21/14.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value=DirectionalLightNode.class, name="DIRECTIONAL_LIGHT"),
        @JsonSubTypes.Type(value=AmbientLightNode.class, name="AMBIENT_LIGHT"),
        @JsonSubTypes.Type(value=CuboidNode.class, name="CUBOID"),
        @JsonSubTypes.Type(value=TorusNode.class, name="TORUS"),
        @JsonSubTypes.Type(value=ModelNode.class, name="MODEL")
})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class SceneNode {
    public static final int NODE_STATE_LENGTH = 2 * 12 + 4 * 3;
    /**
     * Unique identifier of the node.
     */
    @Id
    @Column(nullable = false)
    private String id;
    @Transient
    private UUID uuid;

    /**
     * Unique identifier of the parent node or null if root node.
     */
    @JsonIgnore
    @Column(nullable = true)
    private String parentId;
    @Transient
    private UUID parentUuid;

    @PrePersist
    private void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        id = uuid.toString();
        if (parentUuid != null) {
            parentId = parentUuid.toString();
        }
    }

    @PreUpdate
    private void preUpdate() {
        if (parentUuid != null) {
            parentId = parentUuid.toString();
        }
    }

    @PostLoad
    private void postLoad() {
        uuid = UUID.fromString(id);
        if (parentId != null) {
            parentUuid = UUID.fromString(parentId);
        }
    }


    /**
     * Human readable name of the node.
     */
    @Column(nullable = false)
    private String name;
    /**
     * The node type.
     */
    @Column(nullable = false)
    private NodeType type;
    /**
     * The scene ID.
     */
    @ManyToOne(cascade = { CascadeType.DETACH }, optional = false)
    private Scene scene;
    /** Owning company. */
    @JsonIgnore
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;
    /**
     * Fingerprint of the user owner certificate.
     */
    @Column(nullable = false)
    private String ownerCertificateFingerprint;

    /**
     * Node index in the dynamic object table of the scene.
     */
    @Transient
    private int index;
    /**
     * Node internal state.
     */
    @Column(nullable = false)
    private int state;
    /**
     * Node x coordinate relative to parent.
     */
    @Column(nullable = false)
    private int x;
    /**
     * Node y coordinate relative to parent.
     */
    @Column(nullable = false)
    private int y;
    /**
     * Node z coordinate relative to parent.
     */
    @Column(nullable = false)
    private int z;
    /**
     * Node rotation x relative to parent.
     */
    @Column(nullable = false)
    private int ax;
    /**
     * Node rotation y relative to parent.
     */
    @Column(nullable = false)
    private int ay;
    /**
     * Node rotation z relative to parent.
     */
    @Column(nullable = false)
    private int az;
    /**
     * Node rotation w relative to parent.
     */
    @Column(nullable = false)
    private int aw = 10000;
    /**
     * Node radius;
     */
    @Column(nullable = false)
    private int radius;
    /**
     * Node mass;
     */
    @Column(nullable = false)
    private int mass;
    /**
     * Node state domain animation index from the model animation table. State animation repeats automatically.
     */
    @Column(nullable = false)
    private int stateAnimationIndex = -1;
    /**
     * Node state domain animation rate (frames / second with precision of 1). State animation repeats automatically.
     */
    @Column(nullable = false)
    private int stateAnimationRate;
    /**
     * Node action animation index from the model animation table. State animation does not repeat automatically.
     */
    @Column(nullable = false)
    private int actionAnimationIndex = -1;
    /**
     * Node action animation rate (frames / second with precision of 1). State animation does not repeat automatically.
     */
    @Column(nullable = false)
    private int actionAnimationRate;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * Whether scene node should persist in scene when owner disconnects and when server restarts.
     */
    @Transient
    private boolean persistent=true;

    /**
     * Next interpolation target.
     */
    @Transient
    @JsonIgnore
    private Vector3f nextTargetTranslation = null;
    /**
     * Current interpolation target.
     */
    @Transient
    @JsonIgnore
    private Vector3f currentTargetTranslation = null;
    /**
     * Interpolated interpolation target.
     */
    @Transient
    @JsonIgnore
    private Vector3f interpolatedTargetTranslation = null;
    /**
     * Interpolated translation.
     */
    @Transient
    @JsonIgnore
    private Vector3f interpolatedTranslation = null;
    /**
     * Rotation target.
     */
    @Transient
    @JsonIgnore
    private Quaternion targetRotation = null;
    /**
     * Interpolated rotation target.
     */
    @Transient
    @JsonIgnore
    private Quaternion interpolatedTargetRotation = null;
    /**
     * Interpolated rotation.
     */
    @Transient
    @JsonIgnore
    private Quaternion interpolatedRotation = null;

    public SceneNode() {
        this.type = NodeType.UNDEFINED;
    }

    protected SceneNode(final NodeType type) {
        this.type = type;
    }

    protected SceneNode(String ownerCertificateFingerprint, UUID id, UUID parentId, String name, String model, int index, int state, int x, int y, int z, int radius, int ax, int ay, int az, int aw, int stateAnimationIndex, int stateAnimationRate, int spatialAnimationIndex, int spatialAnimationRate) {
        this.type = NodeType.UNDEFINED;
        this.ownerCertificateFingerprint = ownerCertificateFingerprint;
        this.uuid = id;
        this.parentUuid = parentId;
        this.name = name;
        this.index = index;
        this.state = state;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.aw = aw;
        this.stateAnimationIndex = stateAnimationIndex;
        this.stateAnimationRate = stateAnimationRate;
        this.actionAnimationIndex = spatialAnimationIndex;
        this.actionAnimationRate = spatialAnimationRate;
    }

    protected void copyFrom(final SceneNode node) {
        this.type = node.getType();
        this.ownerCertificateFingerprint = node.getOwnerCertificateFingerprint();
        this.uuid = node.getId();
        this.parentUuid = node.getParentId();
        this.name = node.getName();
        this.index = node.getIndex();
        this.state = node.getState();
        this.x = node.getX();
        this.y = node.getY();
        this.z = node.getZ();
        this.radius = node.getRadius();
        this.ax = node.getAx();
        this.ay = node.getAy();
        this.az = node.getAz();
        this.aw = node.getAw();
        this.stateAnimationIndex = node.getStateAnimationIndex();
        this.stateAnimationRate = node.getStateAnimationRate();
        this.actionAnimationIndex = node.getActionAnimationIndex();
        this.actionAnimationRate = node.getActionAnimationRate();
        this.persistent = node.isPersistent();
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public String getOwnerCertificateFingerprint() {
        return ownerCertificateFingerprint;
    }

    public void setOwnerCertificateFingerprint(String ownerCertificateFingerprint) {
        this.ownerCertificateFingerprint = ownerCertificateFingerprint;
    }

    public UUID getId() {
        return uuid;
    }

    public void setId(UUID id) {
        this.uuid = id;
    }

    public UUID getParentId() {
        return parentUuid;
    }

    public void setParentId(UUID parentId) {
        this.parentUuid = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getMass() {
        return mass;
    }

    public void setMass(int mass) {
        this.mass = mass;
    }

    public int getAx() {
        return ax;
    }

    public void setAx(int ax) {
        this.ax = ax;
    }

    public int getAy() {
        return ay;
    }

    public void setAy(int ay) {
        this.ay = ay;
    }

    public int getAz() {
        return az;
    }

    public void setAz(int az) {
        this.az = az;
    }

    public int getAw() {
        return aw;
    }

    public void setAw(int aw) {
        this.aw = aw;
    }

    public int getStateAnimationIndex() {
        return stateAnimationIndex;
    }

    public void setStateAnimationIndex(int stateAnimationIndex) {
        this.stateAnimationIndex = stateAnimationIndex;
    }

    public int getStateAnimationRate() {
        return stateAnimationRate;
    }

    public void setStateAnimationRate(int stateAnimationRate) {
        this.stateAnimationRate = stateAnimationRate;
    }

    public int getActionAnimationIndex() {
        return actionAnimationIndex;
    }

    public void setActionAnimationIndex(int actionAnimationIndex) {
        this.actionAnimationIndex = actionAnimationIndex;
    }

    public int getActionAnimationRate() {
        return actionAnimationRate;
    }

    public void setActionAnimationRate(int actionAnimationRate) {
        this.actionAnimationRate = actionAnimationRate;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public Company getOwner() {
        return owner;
    }

    public void setOwner(Company owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SceneNode node = (SceneNode) o;

        if (!uuid.equals(node.uuid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + uuid +
                ", name='" + name + '\'' +
                '}';
    }

    public void update(final SceneNode source) {
        setName(source.getName());
        setState(source.getState());
        setX(source.getX());
        setY(source.getY());
        setZ(source.getZ());
        setAx(source.getAx());
        setAy(source.getAy());
        setAz(source.getAz());
        setAw(source.getAw());
        setRadius(source.getRadius());
        setMass(source.getMass());
        setStateAnimationIndex(source.getStateAnimationIndex());
        setStateAnimationRate(source.getStateAnimationRate());
        setActionAnimationIndex(source.getActionAnimationIndex());
        setActionAnimationRate(source.getActionAnimationRate());
        setPersistent(source.isPersistent());
    }

    public void writeState(byte[] bytes, int startIndex) {
        int index = startIndex;
        BytesUtil.writeShort((short) getIndex(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getId().hashCode(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getState(), bytes, index);
        index += 2;
        BytesUtil.writeInteger(getX(), bytes, index);
        index += 4;
        BytesUtil.writeInteger(getY(), bytes, index);
        index += 4;
        BytesUtil.writeInteger(getZ(), bytes, index);
        index += 4;
        BytesUtil.writeShort((short) getRadius(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getAx(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getAy(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getAz(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getAw(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getStateAnimationIndex(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getStateAnimationRate(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getActionAnimationIndex(), bytes, index);
        index += 2;
        BytesUtil.writeShort((short) getActionAnimationRate(), bytes, index);
        index += 2;
        if (NODE_STATE_LENGTH != index - startIndex) {
            throw new RuntimeException("Node state length constant is incorrect, should be: " + (index - startIndex));
        }
    }

    public void readState(byte[] state, int startIndex) {
        int index = startIndex;
        index += 2;
        index += 2;
        setState(BytesUtil.readShort(state, index));
        index += 2;
        setX(BytesUtil.readInteger(state, index));
        index += 4;
        setY(BytesUtil.readInteger(state, index));
        index += 4;
        setZ(BytesUtil.readInteger(state, index));
        index += 4;
        setRadius(BytesUtil.readShort(state, index));
        index += 2;
        setAx(BytesUtil.readShort(state, index));
        index += 2;
        setAy(BytesUtil.readShort(state, index));
        index += 2;
        setAz(BytesUtil.readShort(state, index));
        index += 2;
        setAw(BytesUtil.readShort(state, index));
        index += 2;
        setStateAnimationIndex(BytesUtil.readShort(state, index));
        index += 2;
        setStateAnimationRate(BytesUtil.readShort(state, index));
        index += 2;
        setActionAnimationIndex(BytesUtil.readShort(state, index));
        index += 2;
        setActionAnimationRate(BytesUtil.readShort(state, index));
        index += 2;
        if (NODE_STATE_LENGTH != index - startIndex) {
            throw new RuntimeException("Node state length constant is incorrect, should be: " + (index - startIndex));
        }
    }

    public void setTranslation(final Vector3f translation) {
        setX((int) (translation.x * 1000));
        setY((int) (translation.y * 1000));
        setZ((int) (translation.z * 1000));
    }

    public void setRotation(final Quaternion quaternion) {
        setAx((int) (quaternion.getX() * 10000));
        setAy((int) (quaternion.getY() * 10000));
        setAz((int) (quaternion.getZ() * 10000));
        setAw((int) (quaternion.getW() * 10000));
    }

    @JsonIgnore
    public Vector3f getInterpolatedTranslation() {
        return interpolatedTranslation;
    }

    @JsonIgnore
    public Quaternion getInterpolatedRotation() {
        return interpolatedRotation;
    }

    public void updateInterpolateTarget() {
        final Vector3f translation = new Vector3f(
                getX() / 1000f,
                getY() / 1000f,
                getZ() / 1000f
        );

        if (interpolatedTranslation == null) {
            interpolatedTranslation = new Vector3f(translation);
        }
        if (nextTargetTranslation == null) {
            currentTargetTranslation =  new Vector3f(translation);
        } else {
            currentTargetTranslation =  new Vector3f(nextTargetTranslation);
        }
        interpolatedTargetTranslation =  new Vector3f(currentTargetTranslation);
        nextTargetTranslation = translation;

        final Quaternion rotation = new Quaternion(
                getAx() / 10000f,
                getAy() / 10000f,
                getAz() / 10000f,
                getAw() / 10000f);

        if (interpolatedTargetRotation == null) {
            interpolatedTargetRotation = new Quaternion(rotation);
        }
        if (interpolatedRotation == null) {
            interpolatedRotation = new Quaternion(rotation);
        }
        targetRotation = rotation;
    }

    public boolean interpolate(final float timeDelta) {
        if (nextTargetTranslation == null) {
            return false;
        }
        if (timeDelta == 0) {
            return false;
        }

        Vector3f targetDelta = new Vector3f(nextTargetTranslation);
        targetDelta = targetDelta.subtract(currentTargetTranslation);
        targetDelta = targetDelta.mult(timeDelta / VrConstants.CYCLE_LENGTH_SECONDS);

        interpolatedTargetTranslation = interpolatedTargetTranslation.add(targetDelta);

        Vector3f delta = new Vector3f(interpolatedTargetTranslation);
        delta = delta.subtract(interpolatedTranslation);
        delta = delta.mult(timeDelta / VrConstants.CYCLE_LENGTH_SECONDS);

        interpolatedTranslation = interpolatedTranslation.add(delta);

        interpolatedTargetRotation.slerp(targetRotation, timeDelta / VrConstants.CYCLE_LENGTH_SECONDS);
        interpolatedRotation.slerp(interpolatedTargetRotation, timeDelta / VrConstants.CYCLE_LENGTH_SECONDS);

        return true;
    }

    @Override
    public SceneNode clone() {
        throw new UnsupportedOperationException();
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }
}
