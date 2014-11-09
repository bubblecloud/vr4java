package org.bubblecloud.vr4java.audio;

import javax.sound.sampled.AudioFormat;

/**
 * Created by tlaukkan on 11/9/2014.
 */
public class AudioConstants {
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(8000, 16, 1, true, true);

    public static final int AUDIO_BUFFER_SIZE = 4096;

    public static final int AUDIO_PLAYER_BUFFER_SIZE = AUDIO_BUFFER_SIZE * 10;
}
