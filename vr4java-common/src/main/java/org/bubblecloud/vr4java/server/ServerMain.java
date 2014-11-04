package org.bubblecloud.vr4java.server;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bubblecloud.vr4java.api.SceneService;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.model.SceneNode;
import org.bubblecloud.vr4java.rpc.RpcSealer;
import org.bubblecloud.vr4java.rpc.RpcSealerImpl;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.vaadin.addons.sitekit.jetty.DefaultJettyConfiguration;
import org.vaadin.addons.sitekit.site.DefaultSiteUI;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import javax.persistence.EntityManager;
import javax.websocket.server.ServerContainer;
import java.net.URI;
import java.security.Security;
import java.util.List;

/**
 * Created by tlaukkan on 9/21/14.
 */
public class ServerMain {

    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    /** The persistence unit to be used. */
    public static final String PERSISTENCE_UNIT = "vr4java";
    /** The localization bundle. */
    public static final String LOCALIZATION_BUNDLE = "site-localization";
    /** The web service port. */
    public static final int WS_PORT = 8080;

    /** The server cycle in milliseconds. */
    public static final long CYCLE_LENGTH_MILLIS = 200;
    /** The server cycle in seconds. */
    public static final float CYCLE_LENGTH_SECONDS = CYCLE_LENGTH_MILLIS / 1000f;

    /** Cycle count. */
    private long cycleCount = 0;
    /** Flag indicating shutdown request. */
    private boolean shutdownRequested = false;

    private String serverIdentity;
    private RpcSealer sealer;
    private ServerContext serverContext;
    private SceneRepository sceneRepository;
    private Server server;
    private ServerContainer container;
    private Thread serverThread;
    public static final String PROPERTIES_CATEGORY = "vr4java-server";

    public ServerMain() {

    }

    public void start() throws Exception {

        PropertiesUtil.setCategoryRedirection("site", "vr4java-server");

        // Configuration loading with HEROKU support.
        final String environmentDatabaseString = System.getenv("DATABASE_URL");
        if (StringUtils.isNotEmpty(environmentDatabaseString)) {
            final URI dbUri = new URI(environmentDatabaseString);
            final String dbUser = dbUri.getUserInfo().split(":")[0];
            final String dbPassword = dbUri.getUserInfo().split(":")[1];
            final String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
            PropertiesUtil.setProperty(PROPERTIES_CATEGORY, "javax.persistence.jdbc.url", dbUrl);
            PropertiesUtil.setProperty(PROPERTIES_CATEGORY, "javax.persistence.jdbc.user", dbUser);
            PropertiesUtil.setProperty(PROPERTIES_CATEGORY, "javax.persistence.jdbc.password", dbPassword);
            LOGGER.info("Environment variable defined database URL: " + environmentDatabaseString);
        }

        // Construct Jetty server with default Ilves server configuration.
        server = DefaultJettyConfiguration.configureServer(PERSISTENCE_UNIT, LOCALIZATION_BUNDLE);

        serverIdentity = PropertiesUtil.getProperty("vr4java-server", "server-certificate-self-sign-host-name");
        sealer = new RpcSealerImpl(serverIdentity);
        serverContext = ServerContext.buildLocalServerContext(serverIdentity);
        sceneRepository = new SceneRepository(serverContext);
        sceneRepository.ensureSceneExists();

        final SceneService sceneService = ServerRpcService.getSceneService();
        for (final Scene scene : sceneRepository.loadScenes()) {
            final List<SceneNode> sceneNodes = sceneRepository.loadNodes(scene);
            sceneService.addScene(scene, sceneNodes);
            LOGGER.info("Server loaded " +  sceneNodes.size() + " nodes to scene "
                    + scene.getName() + "(" + scene.getId() + ")");
        }

        final ServletContextHandler webSocketsContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        webSocketsContext.setContextPath("/ws");

        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { server.getHandler(), webSocketsContext });
        server.setHandler(contexts);

        container = WebSocketServerContainerInitializer.configureContext(webSocketsContext);
        container.addEndpoint(ServerRpcService.class);

        server.start();

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mainLoop();
            }
        });
        serverThread.start();
    }

    public void stop() {
        try {
            shutdownRequested = true;
            server.stop();
            serverThread.interrupt();
        } catch (Exception e) {
            LOGGER.error("Error stopping server: " + e);
        }
    }

    public void join() throws InterruptedException {
        server.join();
    }

    /**
     * The main processing loop which runs in cycles.
     */
    private void mainLoop() {
        LOGGER.info("Server main loop started.");
        long lastCycleStartMillis = System.currentTimeMillis();
        try {
            Thread.sleep(CYCLE_LENGTH_MILLIS);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        try {
            while (!shutdownRequested) {

                final long cycleStartTimeMillis = System.currentTimeMillis();
                final long timeDeltaMillis = cycleStartTimeMillis - lastCycleStartMillis;
                long timeExceededMillisLastRound = timeDeltaMillis - CYCLE_LENGTH_MILLIS;

                if (timeExceededMillisLastRound < 0) {
                    timeExceededMillisLastRound = 0;
                }
                if (timeExceededMillisLastRound > CYCLE_LENGTH_MILLIS / 2) {
                    timeExceededMillisLastRound = CYCLE_LENGTH_MILLIS / 2;
                }

                try {
                    cycle(cycleCount, timeDeltaMillis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                lastCycleStartMillis = cycleStartTimeMillis;
                cycleCount++;

                final long cycleEndTimeMillis = System.currentTimeMillis();
                final long cycleTimeMillis = cycleEndTimeMillis - cycleStartTimeMillis;

                if (CYCLE_LENGTH_MILLIS > cycleTimeMillis + timeExceededMillisLastRound) {
                    try {
                        Thread.sleep(CYCLE_LENGTH_MILLIS - cycleTimeMillis - timeExceededMillisLastRound);
                    } catch (final InterruptedException e) {
                        LOGGER.trace("Server main loop sleep interrupted.");
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Error in server main loop.", e);
        } finally {
            LOGGER.info("Server main loop exited.");
        }
    }

    /**
     * The server cycle.
     * @param cycleIndex index of the server cycle.
     * @param timeDeltaMillis time since last cycle start in milliseconds.
     */
    private void cycle(final long cycleIndex, final long timeDeltaMillis) {
        LOGGER.debug("Server main cycle time delta: " + timeDeltaMillis);
        ServerRpcService.sendStateSlugToClients();
    }

    public static void main(final String[] args) throws Exception {
        final ServerMain vrServerMain = new ServerMain();
        vrServerMain.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (vrServerMain != null) {
                        vrServerMain.stop();
                    }
                } catch(final Throwable t) {
                    LOGGER.error("Error stopping VR server.", t);
                }
            }
        }));

        vrServerMain.join();
    }
}
