package org.bubblecloud.vr4java;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.client.ClientNetwork;
import org.bubblecloud.vr4java.client.ClientNetworkStartupListener;
import org.bubblecloud.vr4java.ui.*;

import java.io.File;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;

/**
 * Virtual reality for Java main class.
 *
 * @author Tommi S.E. Laukkanen
 */
public class VrClient extends SimpleApplication {
    private static final Logger LOGGER = Logger.getLogger(VrClient.class.getName());

    private final ClientNetwork clientNetwork;
    private SceneContext sceneContext;

    public static void main(String[] args) {
        try {
            java.util.logging.Logger.getLogger("").setLevel(Level.SEVERE);

            final File oldInstaller = new File("installer.jar");
            final File newInstaller = new File("installer-new.jar");

            if (newInstaller.exists()) {
                if (oldInstaller.exists() && newInstaller.lastModified() > oldInstaller.lastModified()) {
                    LOGGER.info("Deleting installer from the way of new installer.");
                    Thread.sleep(100);
                    oldInstaller.delete();
                    Thread.sleep(100);
                }
                if (!oldInstaller.exists()) {
                    LOGGER.info("Renaming new installer as installer.");
                    newInstaller.renameTo(oldInstaller);
                    if (oldInstaller.exists()) {
                        LOGGER.info("Re-executing installer.");
                        Runtime.getRuntime().exec("java -jar installer.jar");
                        return;
                    }
                }
            }

            final VrSplash vrSplash = new VrSplash();

            final ClientNetwork clientNetwork = new ClientNetwork();
            try {
                clientNetwork.start(new ClientNetworkStartupListener() {
                    @Override
                    public void message(String message) {
                        vrSplash.render(message);
                    }
                });
            } catch (final Exception e) {
                LOGGER.error("Error connecting to server.", e);
                return;
            }

            vrSplash.close();
            final AppSettings appSettings = new AppSettings(true);
            appSettings.setSamples(8);
            appSettings.setVSync(true);

            try {
                appSettings.load("vr4java");
            } catch (BackingStoreException e) {
                LOGGER.info("Error loading settings", e);
            }

            final VrClient app = new VrClient(clientNetwork);
            app.setSettings(appSettings);
            app.setShowSettings(true);
            app.start();

            final AppState shutdownAppState = new AbstractAppState() {
                @Override
                public void cleanup() {
                    try {
                        appSettings.save("vr4java");
                    } catch (BackingStoreException e) {
                        LOGGER.info("Error saving settings", e);
                    }
                    clientNetwork.stop();
                }
            };
            shutdownAppState.setEnabled(true);
            app.getStateManager().attach(shutdownAppState);
        } catch (final Throwable t) {
            LOGGER.error("Error in VR client startup.", t);
        }
    }

    public VrClient(ClientNetwork clientNetwork) {
        this.clientNetwork = clientNetwork;
    }

    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);
        setPauseOnLostFocus(false);
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(ColorRGBA.White);
        final BulletAppState physicsState = new BulletAppState();
        stateManager.attach(physicsState);
        //physicsState.setDebugEnabled(true);
        assetManager.registerLocator("assets", FileLocator.class);

        sceneContext = new SceneContext();
        sceneContext.setAudioRecordController(new AudioRecordController(sceneContext));
        sceneContext.setAudioPlaybackController(new AudioPlaybackController(sceneContext));
        sceneContext.setClientNetwork(clientNetwork);
        sceneContext.setInputManager(inputManager);
        sceneContext.setAssetManager(assetManager);
        sceneContext.setCamera(cam);
        sceneContext.setRootNode(rootNode);
        sceneContext.setPhysicsSpace(physicsState.getPhysicsSpace());

        sceneContext.setSteeringController(new SteeringController(sceneContext));

        sceneContext.setSceneController(new SceneController(sceneContext));
        sceneContext.setEditController(new EditController(sceneContext));
        sceneContext.getSceneController().loadScene();
        sceneContext.setSpeechSynthesiser(new SpeechSynthesiser());

        sceneContext.setAide(new Aide(sceneContext));

        final org.bubblecloud.vr4java.ui.Character character = sceneContext.getSceneController().setupCharacter();
        sceneContext.setCharacter(character);
        sceneContext.getSteeringController().setCharacter(character);
    }

    @Override
    public void simpleUpdate(float tpf) {
        sceneContext.getSteeringController().update(tpf);
        sceneContext.getEditController().update(tpf);
        sceneContext.getSceneController().update(tpf);
        sceneContext.getCharacter().getCharacterAnimator().update(tpf);
    }

    @Override
    public void simpleRender(final RenderManager renderManager) {
    }

}
