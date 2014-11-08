package vr4java;

import org.apache.log4j.Logger;

import java.awt.*;

public class VrSplash {
    private static final Logger LOGGER = Logger.getLogger(VrSplash.class.getName());
    private static final int FONT_HEIGHT = 18;
    private SplashScreen splash;
    private Graphics2D graphics;
    private final Font font;

    public VrSplash() {
        super();
        font = new Font("Dialog", Font.PLAIN, FONT_HEIGHT);
        splash = SplashScreen.getSplashScreen();
        if (splash == null) {
            LOGGER.warn("SplashScreen.getSplashScreen() returned null.");
            return;
        }
        graphics = splash.createGraphics();
        if (graphics == null) {
            LOGGER.warn("Splash graphics is null.");
            return;
        }
    }

    public void close() {
        if (splash == null || graphics == null) {
            return;
        }
        splash.close();
    }


    void render(final String message) {
        if (splash == null || graphics == null) {
            LOGGER.info(message);
            return;
        }
        final Rectangle bounds = splash.getBounds();
        final int width = bounds.width;
        final int height = 40;
        final int x = bounds.width / 8;
        final int y = bounds.height / 2 - height / 2;
        final String[] comps = {"foo", "bar", "baz"};
        graphics.setColor(new Color(1f, 1f, 1f, 1f));
        graphics.fillRect(x, y, width, 40);
        graphics.setPaintMode();
        graphics.setFont(font);
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        graphics.setColor(Color.DARK_GRAY);
        graphics.drawString(message, x + FONT_HEIGHT / 4, bounds.height / 2 + FONT_HEIGHT / 2.5f);
        splash.update();
    }

    public static void main (String args[]) throws InterruptedException {
        VrSplash vrSplash = new VrSplash();
        for(int i=0; i<30; i++) {
            vrSplash.render("Loading...");
            try {
                Thread.sleep(100);
            }
            catch(InterruptedException e) {
            }
        }
        vrSplash.close();
    }
}
