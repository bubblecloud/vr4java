package org.bubblecloud.vr4java.ui;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.audio.AudioPlayer;
import org.bubblecloud.vr4java.audio.AudioRecorder;

/**
 * Created by tlaukkan on 11/9/2014.
 */
public class AudioRecordController {
    private static final Logger LOGGER = Logger.getLogger(AudioRecordController.class.getName());

    private final SceneContext sceneContext;
    private AudioRecorder audioRecorder;

    public AudioRecordController(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
    }

    public void beginAudioRecord() {
        if (audioRecorder != null) {
            LOGGER.warn("Already recording audio.");
            return;
        }
        try {
            audioRecorder = new AudioRecorder(new AudioPlayer() {
                @Override
                public void write(byte[] b, int off, int len) {
                    LOGGER.debug("Recorded audio bytes: " + len);
                }
            });
            audioRecorder.start();
        } catch (final Exception e) {
            LOGGER.error("Error starting audio recording.", e);
        }
        LOGGER.info("Audio recording started.");
    }

    public void endAudioRecord() {
        if (audioRecorder == null) {
            LOGGER.warn("Not recording audio.");
            return;
        }
        audioRecorder.stop();
        LOGGER.info("Audio recording stopped.");
    }
}
