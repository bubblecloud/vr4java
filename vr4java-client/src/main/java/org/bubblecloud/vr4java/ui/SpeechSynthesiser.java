package org.bubblecloud.vr4java.ui;

import javazoom.jl.player.Player;
import org.apache.commons.io.IOUtils;
import org.bubblecloud.vr4java.util.GoogleSpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.Character;
import java.util.Arrays;
import java.util.UUID;

/**
 * The shield maiden main class.
 */
public class SpeechSynthesiser {

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechSynthesiser.class);

    public SpeechSynthesiser() {

    }


    public void say(final String language, final String sentence) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saySynchronous(sentence, language);
            }
        }).start();
    }

    public void saySynchronous(String sentence, String language) {
        final String sayFilePath = PropertiesUtil.getProperty("vr4java-client", "sound-cache-path");

        final File sayFileDirectory = new File(sayFilePath);

        if (!sayFileDirectory.exists()) {
            sayFileDirectory.mkdirs();
        }

        final StringBuilder fileNameBuilder = new StringBuilder(sayFileDirectory.getAbsolutePath());
        fileNameBuilder.append("/");
        for (final char c : sentence.toCharArray()) {
            if (Character.isLetterOrDigit(c) ||
                "!.,? ".indexOf(c) != -1) {
                if (c == ' ') {
                    fileNameBuilder.append('_');
                } else {
                    fileNameBuilder.append(Character.toLowerCase(c));
                }
            }
        }

        final File utteranceSoundFile = new File(fileNameBuilder.toString());


        if (!utteranceSoundFile.exists()) {
            try {
                LOGGER.info("Using google tts service to generate mp3 for: " + sentence);

                final GoogleSpeechService speechService = new GoogleSpeechService();
                speechService.setLanguage(language);
                final InputStream inputStream = speechService.getMP3Data(sentence);
                try (FileOutputStream out = new FileOutputStream(utteranceSoundFile)) {
                    IOUtils.copy(inputStream, out);
                }
            } catch (final Exception e) {
                LOGGER.error("Generation utterance file failed: ", e);
                return;
            }
        }

        try {
            final FileInputStream inputStream = new FileInputStream(utteranceSoundFile);
            final Player player = new Player(inputStream);
            player.play();
            player.close();
            inputStream.close();
            return;
        } catch (final Exception e) {
            LOGGER.error("Playing utterance file failed: ", e);
            return;
        }
    }
}
