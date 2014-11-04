package org.bubblecloud.vr4java.rpc;

import junit.framework.Assert;

import java.security.cert.X509Certificate;

@javax.websocket.server.ServerEndpoint(value="/rpc/")
public class TestWsServerEndpoint extends RpcWsServerEndpoint {

    private static TestWsServerEndpoint singleton;

    public static TestWsServerEndpoint getSingleton() {
        return singleton;
    }

    public static void setSingleton(TestWsServerEndpoint singleton) {
        TestWsServerEndpoint.singleton = singleton;
    }

    private static EchoServiceImpl serverEchoService = new EchoServiceImpl("server");

    public TestWsServerEndpoint() {
        super();
        singleton = this;
    }

    @Override
    public String getIdentifier() {
        return "test-echo-server";
    }

    @Override
    public Object getRpcHandler() {
        return serverEchoService;
    }

    @Override
    public MessageHandler getMessageHandler() {
        return serverEchoService;
    }

    @Override
    public void onConnect(String remoteFingerprint, X509Certificate remoteCertificate) {

    }

    @Override
    public void onDisconnect(String remoteFingerprint, X509Certificate remoteCertificate) {

    }

    public void testReverseEcho() {
        final EchoService echoTest = RpcProxyUtil.createClientProxy(EchoService.class.getClassLoader(), EchoService.class,
                getRpcEndpoint());
        final String response = echoTest.echo("echo-test");
        Assert.assertEquals("client:echo-test", response);
    }
}