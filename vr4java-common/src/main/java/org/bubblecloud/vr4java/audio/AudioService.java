package org.bubblecloud.vr4java.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioService {
    public static void main(String args[]) throws Exception {

        final AudioPlayerImpl audioPlayer = new AudioPlayerImpl();

        final AudioRecorder audioRecorder = new AudioRecorder(audioPlayer);

        /*final AudioFormat format = new AudioFormat(8000, 16, 1, true, true);
        final int bufferSize = (int) format.getSampleRate() * format.getFrameSize();

        DataLine.Info targetLineInfo = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(targetLineInfo)) {
            System.out.println("Target line not supported");
            return;
        }

        final TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(targetLineInfo);
        targetDataLine.open(format);
        targetDataLine.start();
        Runnable runner = new Runnable() {
            public void run() {
                byte buffer[] = new byte[bufferSize];
                while(true) {
                    int count = targetDataLine.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        audioPlayer.write(buffer, 0, count);
                    }
                    System.out.println("Recorded: " + count);
                }
            }
        };

        final Thread captureThread = new Thread(runner);
        captureThread.start();*/

        audioRecorder.start();

        audioPlayer.start();

        Thread.sleep(20000);

        audioRecorder.stop();

        audioPlayer.stop();
    }
}