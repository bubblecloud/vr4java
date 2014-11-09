package org.bubblecloud.vr4java.util;

/**
 * Created by tlaukkan on 11/6/2014.
 */
public class VrConstants {
    /** The server cycle in milliseconds. */
    public static final long CYCLE_LENGTH_MILLIS = 200; // 200 ms
    /** The server cycle in seconds. */
    public static final float CYCLE_LENGTH_SECONDS = CYCLE_LENGTH_MILLIS / 1000f; // 0.2 seconds
    /** Grid step for translations when editing objects. */
    public static final float GRID_STEP_TRANSLATION = 0.1f; // 1 cm
    /** Grid step for rotations when editing objects. */
    public static final float GRID_STEP_ROTATION = (float) (5f / 180f * Math.PI); // 5 degrees
}
