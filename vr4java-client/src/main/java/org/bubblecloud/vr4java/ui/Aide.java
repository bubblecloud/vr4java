package org.bubblecloud.vr4java.ui;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.util.VrClientProperties;

/**
 * Created by tlaukkan on 11/15/2014.
 */
public class Aide implements HudController.HudInputCallback, SpeechSynthesiser.SpeechSynthesiserCallback {
    private static final Logger LOGGER = Logger.getLogger(Aide.class.getName());

    public static final String INPUT_USERNAME = "input_username";
    public static final String SPEECH_REQUEST_USERNAME = "speech_request_username";
    public static final String SPEECH_WELCOME = "speech_welcome";
    public static final String SPEECH_RECONNECT_WITH_NEW_IDENTITY = "speech_reconnect_with_new_identity";
    private SceneContext sceneContext;

    public Aide(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
    }

    public void onCharacterLoaded() {
        final String userName = sceneContext.getClientNetwork().getUserName().split(" ")[0];
        if (userName.equals("Anonymous")) {
            sceneContext.getSpeechSynthesiser().say("en_gb", "Welcome! I am your aide process. Please tell me your name.", SPEECH_REQUEST_USERNAME, this);
            sceneContext.getHudController().askUserInput(INPUT_USERNAME, "Your username:", this);
        } else {
            sceneContext.getSpeechSynthesiser().say("en_gb", "Hi, " + userName, SPEECH_WELCOME, this);
        }
    }

    public void update(float tpf) {

    }

    @Override
    public void onInput(final String inputId, final  String inputValue) {
        if (inputId.equals(INPUT_USERNAME)) {
            final String userNamePropertyKey = "user-name";
            final String userName = inputValue.split(" ")[0];
            VrClientProperties.save(userNamePropertyKey, inputValue);
            sceneContext.getSpeechSynthesiser().say("en_gb", "Hi, " + userName + ". Stand by for reconnect with your new identity! 3 2 1 bye", SPEECH_RECONNECT_WITH_NEW_IDENTITY, this);
        }
    }

    @Override
    public void onSpeechSpoken(String speechId) {
        if (speechId.equals(SPEECH_RECONNECT_WITH_NEW_IDENTITY)) {
            sceneContext.getVrClient().restart();
        }
    }
}
