package org.bubblecloud.vr4java.rpc;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.server.ServerContainer;
import java.net.URI;

/**
 * Created by tlaukkan on 9/20/14.
 */
public class RpcTest {

    private static final Logger LOGGER = Logger.getLogger(RpcTest.class.getName());

    public static final int WS_PORT = 8089;
    private Server server;
    private ServerContainer container;

    @Before
    public void before() throws Exception {

        server = new Server();

        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(WS_PORT);
        server.addConnector(connector);

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        container = WebSocketServerContainerInitializer.configureContext(context);
        container.addEndpoint(TestWsServerEndpoint.class);

        server.start();
    }

    @After
    public void after() throws Exception {
        server.stop();
    }

    @Test
    public void testEcho() throws Exception {
        final URI uri = URI.create("ws://localhost:" + WS_PORT + "/rpc/");

        final EchoServiceImpl clientEchoService = new EchoServiceImpl("client");
        final RpcWsClient client = new RpcWsClient("test-echo-client", uri, clientEchoService, clientEchoService);
        client.start();

        final EchoService echoTest = RpcProxyUtil.createClientProxy(EchoService.class.getClassLoader(), EchoService.class, client.getRpcEndpoint());

        final int echoTestCount = 100;
        final String echoTestMessage = "echo-test";
        final long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < echoTestCount; i++) {
            final String response = echoTest.echo(echoTestMessage);
            Assert.assertEquals("server:" + echoTestMessage, response);
            echoTest.event("event-test");
            client.getRpcEndpoint().sendMessage((byte) 20, "message-test".getBytes());
            TestWsServerEndpoint.getSingleton().testReverseEcho();
        }
        LOGGER.debug(echoTestCount + " echo tests took " + (System.currentTimeMillis() - startTimeMillis) + " ms. " +
                "(" + ((System.currentTimeMillis() - startTimeMillis) / (float) echoTestCount) + " ms per echo.)");
        client.stop();
        Assert.assertEquals(echoTestCount, EchoServiceImpl.getEventCount());
        Assert.assertEquals(echoTestCount, EchoServiceImpl.getMessageCount());
    }
}
