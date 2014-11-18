package org.bubblecloud.vr4java.ui;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.util.Genderize;
import org.bubblecloud.vr4java.util.VrClientProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tlaukkan on 11/15/2014.
 */
public class Aide implements HudController.HudInputCallback, SpeechSynthesiser.SpeechSynthesiserCallback {
    private static final Logger LOGGER = Logger.getLogger(Aide.class.getName());

    public static final String INPUT_USERNAME = "input_username";
    public static final String SPEECH_REQUEST_USERNAME = "speech_request_username";
    public static final String SPEECH_WELCOME = "speech_welcome";
    public static final String SPEECH_RECONNECT_WITH_NEW_IDENTITY = "speech_reconnect_with_new_identity";
    public static final String SPEECH_REMOTE_CONNECT = "speech_remote_connect";
    public static final String SPEECH_REMOTE_CHARACTERS = "speech_remote_characters";
    private SceneContext sceneContext;
    private String userName = null;
    private List<String> remoteCharacters = new ArrayList<>();

    public Aide(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
    }

    public void onCharacterLoaded() {
        userName = sceneContext.getClientNetwork().getUserName().split(" ")[0];
        if (userName.equals("Anonymous")) {
            sceneContext.getSpeechSynthesiser().say("en_gb", "Welcome! I am your aide process. Please tell me your name.", SPEECH_REQUEST_USERNAME, this);
        } else {
            sceneContext.getSpeechSynthesiser().say("en_gb", "Hi, " + userName, SPEECH_WELCOME, this);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (remoteCharacters.size() > 0) {
                reportRemoteCharacters();
            }
        }
    }

    private void reportRemoteCharacters() {
        final StringBuilder remoteCharacterNames = new StringBuilder();
        for (final String remoteCharacter : remoteCharacters) {
            if (remoteCharacterNames.length() > 0) {
                remoteCharacterNames.append(',');
            }
            remoteCharacterNames.append(remoteCharacter);
        }
        sceneContext.getSpeechSynthesiser().say("en_gb", "The following persons are already connected: " + remoteCharacterNames, SPEECH_REMOTE_CHARACTERS, this);
    }

    public void onRemoteCharacterLoaded(final String name) {
        if (userName != null) {
            sceneContext.getSpeechSynthesiser().say("en_gb", name + " connected.", SPEECH_REMOTE_CONNECT, this);
        }
        remoteCharacters.add(name);
    }

    public void update(float tpf) {

    }

    @Override
    public void onInput(final String inputId, final  String inputValue) {
        if (inputId.equals(INPUT_USERNAME)) {
            final String userName = inputValue.split(" ")[0];
            final boolean male = Genderize.isMale(userName);
            VrClientProperties.save("user-name", inputValue);
            if (male) {
                VrClientProperties.save("character-model", "jme3-open-asset-pack-v1/character/human/male/ogre/male.scene");
            } else {
                VrClientProperties.save("character-model", "jme3-open-asset-pack-v1/character/human/female/ogre/female.scene");
            }
            sceneContext.getSpeechSynthesiser().say("en_gb", "Hi, " + userName + ". Stand by for reconnect with your new identity! 3 2 1 bye", SPEECH_RECONNECT_WITH_NEW_IDENTITY, this);
        }
    }

    @Override
    public void onSpeechSpoken(String speechId) {
        if (speechId.equals(SPEECH_REQUEST_USERNAME)) {
            sceneContext.getHudController().askUserInput(INPUT_USERNAME, "Your username:", this);
        } else if (speechId.equals(SPEECH_RECONNECT_WITH_NEW_IDENTITY)) {
            sceneContext.getVrClient().restart();
        }
    }
}
