package org.bubblecloud.vr4java.audio;

import org.apache.log4j.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by tlaukkan on 11/9/2014.
 */
public class AudioPlayerImpl implements AudioPlayer {
    private static final Logger LOGGER = Logger.getLogger(AudioPlayerImpl.class.getName());

    private final PipedOutputStream pipedOutputStream;
    private final PipedInputStream pipedInputStream;
    private final SourceDataLine sourceDataLine;

    private boolean started = false;
    private boolean shutdownRequested = false;
    private final Thread playThread;

    public AudioPlayerImpl() throws Exception {
        pipedOutputStream = new PipedOutputStream();
        pipedInputStream = new PipedInputStream(pipedOutputStream, AudioConstants.AUDIO_PLAYER_BUFFER_SIZE);

        final DataLine.Info sourceLineInfo = new DataLine.Info(SourceDataLine.class,
                AudioConstants.AUDIO_FORMAT);
        if (!AudioSystem.isLineSupported(sourceLineInfo)) {
            throw new IOException("Source line not supported");
        }
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(sourceLineInfo);
        sourceDataLine.open(AudioConstants.AUDIO_FORMAT);

        playThread = new Thread(new Runnable() {
            public void run() {
                process();
            }
        });
    }

    public void start() {
        if (started) {
            throw new RuntimeException("Already started");
        }
        sourceDataLine.start();
        playThread.start();
        started = true;
    }

    public void stop() {
        if (shutdownRequested) {
            throw new RuntimeException("Already stopped");
        }
        shutdownRequested = true;
    }

    @Override
    public void write(byte b[], int off, int len) {
        try {
            pipedOutputStream.write(b, off, len);
        } catch (IOException e) {
            LOGGER.error("Error writing audio bytes to player.", e);
        }
    }

    private void process() {
        try {
            byte buffer[] = new byte[AudioConstants.AUDIO_BUFFER_SIZE];
            int count = 1;
            Thread.sleep(1000); // Buffering
            while ((count > 0 || !shutdownRequested) && (count = pipedInputStream.read(buffer, 0, buffer.length)) != -1) {
                if (count > 0) {
                    //LOGGER.info("Playing audio: " + count + " with buffer left: " + pipedInputStream.available());
                    sourceDataLine.write(buffer, 0, count);
                } else {
                    Thread.sleep(100);
                }
            }
            sourceDataLine.drain();
            sourceDataLine.close();
        } catch (final Exception e) {
            LOGGER.error("Error in audio playback.", e);
        }
    }

}
