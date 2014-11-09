package org.bubblecloud.vr4java.audio;

import org.apache.log4j.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;

/**
 * Created by tlaukkan on 11/9/2014.
 */
public class AudioRecorder {
    private static final Logger LOGGER = Logger.getLogger(AudioRecorder.class.getName());

    private final AudioPlayer audioPlayer;
    private final TargetDataLine targetDataLine;
    private final Thread recordThread;

    private boolean started = false;
    private boolean shutdownRequested = false;

    public AudioRecorder(final AudioPlayer audioPlayer) throws Exception {
        this.audioPlayer = audioPlayer;

        DataLine.Info targetLineInfo = new DataLine.Info(TargetDataLine.class, AudioConstants.AUDIO_FORMAT);
        if (!AudioSystem.isLineSupported(targetLineInfo)) {
            throw new IOException("Target line not supported");
        }

        targetDataLine = (TargetDataLine) AudioSystem.getLine(targetLineInfo);
        targetDataLine.open(AudioConstants.AUDIO_FORMAT);

        recordThread = new Thread(new Runnable() {
            public void run() {
                process();
            }
        });
    }

    public void start() {
        if (started) {
            throw new RuntimeException("Already started");
        }
        targetDataLine.start();
        recordThread.start();
        started = true;
    }

    public void stop() {
        if (shutdownRequested) {
            throw new RuntimeException("Already stopped");
        }
        shutdownRequested = true;
        recordThread.interrupt();
        try {
            recordThread.join();
        } catch (final InterruptedException e) {
            LOGGER.error("Interrupted while waiting audio record thread to exit.");
        }
    }

    private void process() {
        byte buffer[] = new byte[AudioConstants.AUDIO_BUFFER_SIZE];
        while(!shutdownRequested) {
            int count = targetDataLine.read(buffer, 0, buffer.length);
            if (count > 0) {
                LOGGER.info("Recorded audio: " + count + " bytes.");
                audioPlayer.write(buffer, 0, count);
            }
        }
        targetDataLine.stop();
    }

}
