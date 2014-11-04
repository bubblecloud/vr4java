/*
The MIT License (MIT)

Copyright (c) 2014 jsonrpc4j

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package org.bubblecloud.vr4java.rpc;

import java.io.*;
import java.net.URI;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.apache.log4j.Logger;

import javax.websocket.*;

/**
 * A JSON-RPC client that uses the web socket protocol.
 */
@ClientEndpoint
public class RpcWsClient {
    private static final Logger LOGGER = Logger.getLogger(RpcWsClient.class.getName());

    private final RpcSealer sealer;
    private final URI uri;
    private final WebSocketContainer container;
    private final JsonRpcServer server;
    private final MessageHandler messageHandler;
    private Session session;
    private RpcEndpoint rpcEndpoint;

    /**
     * The server URI.
     * @param uri the uri
     * @param rpcHandler
     * @param messageHandler
     */
	public RpcWsClient(final String identity, URI uri, Object rpcHandler, MessageHandler messageHandler) {
		super();
        this.uri = uri;
        this.server = new JsonRpcServer(new ObjectMapper().configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true), rpcHandler);
        this.messageHandler = messageHandler;
        this.container = ContainerProvider.getWebSocketContainer();
        sealer = new RpcSealerImpl(identity);
	}

    /**
     * Starts RPC client.
     * @throws IOException if IO exception occurs during start.
     * @throws DeploymentException id deployment exception occurs during start.
     */
    public void start() throws IOException, DeploymentException, InterruptedException {
        session = container.connectToServer(this, uri);
        rpcEndpoint = new RpcEndpoint(session, server, messageHandler, sealer);
        rpcEndpoint.start();
    }

    /**
     * Stops RPC client.
     * @throws IOException if IO exception occurs during session stop.
     */
    public void stop() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
        }
        session = null;
        rpcEndpoint = null;
    }

    public RpcEndpoint getRpcEndpoint() {
        return rpcEndpoint;
    }

    @OnOpen
    public void onWebSocketConnect(final Session session) {
        LOGGER.trace("Client socket connected: " + session);
    }

    @OnMessage
    public void onWebSocketBinary(final byte[] b, final boolean last, final Session session) {
        final RpcEndpoint rpcEndpointTemp = rpcEndpoint;
        if (rpcEndpointTemp!= null) {
            rpcEndpointTemp.onWebSocketBinary(b, last);
        }
    }

    @OnClose
    public void onWebSocketClose(final CloseReason reason) {
        LOGGER.trace("Client socket closed: " + reason);
    }

    @OnError
    public void onWebSocketError(final Throwable cause) {
        LOGGER.error("Client socket error.", cause);
    }

    public RpcSealer getSealer() {
        return sealer;
    }
}