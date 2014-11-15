package org.bubblecloud.vr4java.util;

import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;

/**
 * Conversion class.
 */
public class C {
    public static org.bubblecloud.vecmath.Vector3f c(Vector3f v) {
        return new org.bubblecloud.vecmath.Vector3f(v.x,v.y,v.z);
    }

    public static Vector3f c(org.bubblecloud.vecmath.Vector3f v) {
        return new Vector3f(v.x,v.y,v.z);
    }

    public static org.bubblecloud.vecmath.Quaternion c(Quaternion v) {
        return new org.bubblecloud.vecmath.Quaternion(v.getX(), v.getY(), v.getZ(), v.getW());
    }

    public static Quaternion c(org.bubblecloud.vecmath.Quaternion v) {
        return new Quaternion(v.getX(), v.getY(), v.getZ(), v.getW());
    }
}
