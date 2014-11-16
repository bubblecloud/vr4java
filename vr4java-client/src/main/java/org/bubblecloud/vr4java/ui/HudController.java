package org.bubblecloud.vr4java.ui;

import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.KeyInputHandler;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import org.apache.log4j.Logger;

/**
 * Created by tlaukkan on 11/15/2014.
 */
public class HudController implements ScreenController, KeyInputHandler {
    private static final Logger LOGGER = Logger.getLogger(HudController.class.getName());

    private final Nifty nifty;
    private NiftyJmeDisplay niftyJmeDisplay;
    private final Screen startScreen;

    private SceneContext sceneContext;

    private TextField input;
    private Element inputField;
    private final Element consolePopup;

    private String inputId = "";
    private HudInputCallback inputCallback;


    public HudController(final SceneContext sceneContext, final NiftyJmeDisplay niftyJmeDisplay) {
        this.sceneContext = sceneContext;
        this.niftyJmeDisplay = niftyJmeDisplay;

        nifty = niftyJmeDisplay.getNifty();
        nifty.registerScreenController(this);
        nifty.fromXml("ui/hud.xml", "start", this);
        startScreen = nifty.getScreen("start");
        consolePopup = nifty.createPopup("console");
        inputField = consolePopup.findElementByName("input");
        inputField.addInputHandler(this);
     }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        LOGGER.info("Hud bind: " + screen.getScreenId());
    }

    @Override
    public void onStartScreen() {
        LOGGER.info("Hud onStartScreen.");
    }

    @Override
    public void onEndScreen() {
        LOGGER.info("Hud onEndScreen.");
    }

    @Override
    public boolean keyEvent(final NiftyInputEvent niftyInputEvent) {
        if (niftyInputEvent == null) {
            return false;
        }
        nifty.closePopup(consolePopup.getId());
        inputCallback.onInput(inputId, input.getDisplayedText());
        input.setText("");
        return true;
    }

    public void askUserInput(final String inputId, final String inputLabel, final HudInputCallback inputCallback) {
        this.inputId = inputId;
        this.inputCallback = inputCallback;
        nifty.showPopup(startScreen, consolePopup.getId(), inputField);
        input = consolePopup.findNiftyControl("input", TextField.class);
    }

    public static interface HudInputCallback {
        void onInput(final String inputId, final String inputValue);
    }
}
