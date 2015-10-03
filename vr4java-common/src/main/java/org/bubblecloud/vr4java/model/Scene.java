package org.bubblecloud.vr4java.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bubblecloud.ilves.model.Company;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * Created by tlaukkan on 9/21/14.
 */
@Entity
public class Scene {
    /**
     * Unique identifier of the node.
     */
    @Id
    @Column(nullable = false)
    private String id;
    @Transient
    private UUID uuid;

    @PrePersist
         private void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        id = uuid.toString();
    }

    @PreUpdate
    private void preUpdate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        id = uuid.toString();
    }

    @PostLoad
    private void postLoad() {
        uuid = UUID.fromString(id);
    }

    /**
     * Human readable name of the node.
     */
    @Column(nullable = false, unique = true)
    private String name;
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
     * SceneModel global root X coordinate in meters.
     */
    @Column(nullable = false)
    public long x;
    /**
     * SceneModel global root Y coordinate in meters.
     */
    @Column(nullable = false)
    public long y;
    /**
     * SceneModel global root Z coordinate in meters.
     */
    @Column(nullable = false)
    public long z;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    public String getOwnerCertificateFingerprint() {
        return ownerCertificateFingerprint;
    }

    public void setOwnerCertificateFingerprint(String ownerCertificateFingerprint) {
        this.ownerCertificateFingerprint = ownerCertificateFingerprint;
    }

    public UUID getId() {
        return uuid;
    }

    public void setId(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public long getY() {
        return y;
    }

    public void setY(long y) {
        this.y = y;
    }

    public long getZ() {
        return z;
    }

    public void setZ(long z) {
        this.z = z;
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

        Scene scene = (Scene) o;

        if (!uuid.equals(scene.uuid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "Scene{" +
                "name='" + name + '\'' +
                '}';
    }
}
