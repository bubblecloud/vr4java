package org.bubblecloud.vr4java.ui;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.audio.AudioPlayerImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by tlaukkan on 11/9/2014.
 */
public class AudioPlaybackController {
    private static final Logger LOGGER = Logger.getLogger(AudioPlaybackController.class.getName());

    private final SceneContext sceneContext;

    private Map<UUID, Map<UUID, AudioPlayerImpl>> audioPlayers = new HashMap<UUID, Map<UUID, AudioPlayerImpl>>();

    public AudioPlaybackController(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
    }

    public void playAudio(final UUID sceneId, final UUID nodeId, final byte[] bytes) {
        if (!audioPlayers.containsKey(sceneId)) {
            audioPlayers.put(sceneId, new HashMap<UUID, AudioPlayerImpl>());
        }
        final Map<UUID, AudioPlayerImpl> scenePlayers = audioPlayers.get(sceneId);
        if (bytes.length > 0) {
            if (!scenePlayers.containsKey(nodeId)) {
                try {
                    scenePlayers.put(nodeId, new AudioPlayerImpl());
                    final AudioPlayerImpl audioPlayer = scenePlayers.get(nodeId);
                    audioPlayer.write(bytes, 0, bytes.length);
                    audioPlayer.start();
                    LOGGER.info("Started audio player for node: " + nodeId + " (" + bytes.length + ")");
                } catch (Exception e) {
                    LOGGER.error("Error in audio playback.", e);
                }
            } else {
                final AudioPlayerImpl audioPlayer = scenePlayers.get(nodeId);
                audioPlayer.write(bytes, 0, bytes.length);
                //LOGGER.info("Added bytes to audio player for node: " + nodeId + " (" + bytes.length + ")");
            }
        } else {
            if (scenePlayers.containsKey(nodeId)) {
                final AudioPlayerImpl audioPlayer = scenePlayers.get(nodeId);
                audioPlayer.stop();
                scenePlayers.remove(nodeId);
                LOGGER.info("Stopped audio player for node: " + nodeId);
            }
        }
    }
}
