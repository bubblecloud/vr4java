package org.bubblecloud.vr4java.ui;

/**
 * Created by tlaukkan on 11/15/2014.
 */
public class Aide {

    private SceneContext sceneContext;

    public Aide(final SceneContext sceneContext) {
        this.sceneContext = sceneContext;
    }

    public void onCharacterLoaded() {
        final String userName = sceneContext.getClientNetwork().getUserName().split(" ")[0];
        if (userName.equals("Tommi")) {
            sceneContext.getSpeechSynthesiser().say("en_gb", "Welcome! I am your aide process. Please tell me your name.");
        } else {
            sceneContext.getSpeechSynthesiser().say("en_gb", "Hi, " + userName);
        }
    }
}
