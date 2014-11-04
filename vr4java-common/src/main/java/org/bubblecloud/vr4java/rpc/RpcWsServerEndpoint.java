package org.bubblecloud.vr4java.rpc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.apache.log4j.Logger;

import java.security.cert.X509Certificate;
import javax.websocket.*;

public abstract class RpcWsServerEndpoint {

    private static final Logger LOGGER = Logger.getLogger(RpcWsServerEndpoint.class.getName());

    private final RpcSealer sealer;
    private final JsonRpcServer server;
    //private final Map<Session, RpcEndpoint> endpoints = new HashMap<Session, RpcEndpoint>();
    private Session session;
    private RpcEndpoint rpcEndpoint;

    public RpcWsServerEndpoint() {
        sealer = new RpcSealerImpl(getIdentifier());
        server = new JsonRpcServer(new ObjectMapper().configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true), getRpcHandler());
    }

    public abstract String getIdentifier();

    public abstract Object getRpcHandler();

    public abstract MessageHandler getMessageHandler();

    public abstract void onConnect(final String remoteFingerprint, final X509Certificate remoteCertificate);

    public abstract void onDisconnect(final String remoteFingerprint, final X509Certificate remoteCertificate);

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @OnOpen
    public void onWebSocketConnect(final Session session) {
        LOGGER.trace("Socket Connected: " + session);
        if (this.session != null) {
            throw new RuntimeException("End point already used by session.");
        }
        this.session = session;
        this.rpcEndpoint = new RpcEndpoint(session, server, getMessageHandler(), sealer);
        /*synchronized (endpoints) {
            if (!endpoints.containsKey(session)) {
                endpoints.put(session, new RpcEndpoint(session, server, sealer));
            }
        }*/
    }

    @OnMessage
    public void onWebSocketBinary(final byte[] requestBytes, final boolean last, final Session session) {
        if (this.session == null) {
            throw new RuntimeException("End point not assigned with session.");
        }
        if (!this.session.equals(session)) {
            throw new RuntimeException("End point assigned to different session.");
        }
        byte type = requestBytes[RpcConstants.TYPE_INDEX];

        boolean handshake = type == RpcConstants.TYPE_HANDSHAKE_REQUEST
                || type == RpcConstants.TYPE_HANDSHAKE_RESPONSE;

        rpcEndpoint.onWebSocketBinary(requestBytes, last);

        if (handshake && sealer.getRemoteFingerprint() != null) {
            onConnect(sealer.getRemoteFingerprint(), sealer.getRemoteCertificate());
        }
        /*synchronized (endpoints) {
            if (endpoints.containsKey(session)) {
                endpoints.get(session).onWebSocketBinary(requestBytes, last);
            }
        }*/
    }

    @OnClose
    public void onWebSocketClose(final CloseReason reason) {
        LOGGER.trace("Web socket close: " + reason);
        if (this.session == null) {
            throw new RuntimeException("End point not assigned with session.");
        }
        rpcEndpoint = null;
        if (sealer.getRemoteFingerprint() != null) {
            onDisconnect(sealer.getRemoteFingerprint(), sealer.getRemoteCertificate());
        }
        /*synchronized (endpoints) {
            if (endpoints.containsKey(session)) {
                endpoints.remove(session);
            }
        }*/
    }
    
    @OnError
    public void onWebSocketError(final Throwable cause) {
        LOGGER.error("Web socket error.", cause);
    }

    public RpcEndpoint getRpcEndpoint() {
        return rpcEndpoint;
    }

    public RpcSealer getSealer() {
        return sealer;
    }
}