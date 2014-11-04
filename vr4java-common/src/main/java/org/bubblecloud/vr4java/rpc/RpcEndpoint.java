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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.util.BytesUtil;

import javax.websocket.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

/**
 * A JSON-RPC client that uses the web socket protocol.
 */
@ClientEndpoint
public class RpcEndpoint extends JsonRpcClient {

    private static final Logger LOGGER = Logger.getLogger(RpcEndpoint.class.getName());
    private static final long RPC_TIMEOUT_MILLIS = 10000;

    private final JsonRpcServer server;
    private final MessageHandler messageHandler;
    private final RpcSealer sealer;
    private Session session;
    private byte[] responseBytes;
    private int idCounter;
    private int expectedId = -1;


    /**
     * Constructs RPC session.
     *
     * @param session
     * @param sealer
     */
    public RpcEndpoint(final Session session, final JsonRpcServer server, final MessageHandler messageHandler, final RpcSealer sealer) {
		super(new ObjectMapper().configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true));
        this.session = session;
        this.server = server;
        this.messageHandler = messageHandler;
        this.sealer = sealer;
	}

    public void start() throws IOException, InterruptedException {
        synchronized (session) {
            final byte[] requestBytes = sealer.getHandshake(true);
            LOGGER.trace(sealer.getIdentity() + " sent request: " + (byte) requestBytes[1]);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(requestBytes));
            session.wait(RPC_TIMEOUT_MILLIS);
            if (responseBytes == null) {
                throw new IOException("RPC handshake timeout.");
            }
            sealer.verifyHandshake(responseBytes);
        }
    }

    /**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @see com.googlecode.jsonrpc4j.JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param argument the arguments to the method
	 * @param returnType the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	public Object invoke(String methodName, Object argument, Type returnType, boolean event)
		throws Throwable {

        if (session == null) {
            throw new IOException("RPC client not started.");
        }
        if (!session.isOpen()) {
            LOGGER.warn("Ignored RPC call due to client disconnect: " + methodName);
            return null;
        }

        synchronized (session) {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte protocolVersion = RpcConstants.VERSION;
            byte messageType = event ? (byte) RpcConstants.TYPE_EVENT : (byte) RpcConstants.TYPE_REQUEST;
            int messageId = ++idCounter;

            writerHeader(protocolVersion, messageType, messageId, outputStream);

            super.invoke(methodName, argument, outputStream);

            final byte[] requestBytes = outputStream.toByteArray();

            sealer.seal(requestBytes);

            LOGGER.trace(sealer.getIdentity() + " sent request type: " + (byte) requestBytes[1] + " id: " + messageId);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(requestBytes));

            if (event) {
                return null;
            } else {
                expectedId = messageId;
                session.wait(RPC_TIMEOUT_MILLIS);
                expectedId = -1;

                if (responseBytes == null) {
                    throw new IOException("RPC timeout.");
                }

                if (!sealer.verify(responseBytes)) {
                    throw new IOException("RPC response signature validation failed.");
                }

                final ByteArrayInputStream inputStream = new ByteArrayInputStream(responseBytes, RpcConstants.HEADER_LENGTH,
                        responseBytes.length - RpcConstants.HEADER_LENGTH);
                responseBytes = null;
                return super.readResponse(returnType, inputStream);
            }
        }
	}

    /**
     * Send custom message to server.
     * @param messageType the message byte
     * @param message the message
     * @throws Throwable
     */
    public void sendMessage(final byte messageType, final byte[] message) throws IOException {

        if (messageType < 10) {
            throw new IllegalArgumentException("Message types smaller than 20 are reserved for framework.");
        }
        if (session == null) {
            throw new IOException("RPC client not started.");
        }
        if (!session.isOpen()) {
            throw new IOException("RPC client disconnected.");
        }

        synchronized (session) {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte protocolVersion = RpcConstants.VERSION;
            int messageId = ++idCounter;

            writerHeader(protocolVersion, messageType, messageId, outputStream);
            outputStream.write(message);

            final byte[] requestBytes = outputStream.toByteArray();

            sealer.seal(requestBytes);

            LOGGER.trace(sealer.getIdentity() + " sent request type: " + (byte) requestBytes[1] + " id: " + messageId);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(requestBytes));
        }
    }


    public void onWebSocketBinary(final byte[] bytes, final boolean last) {
        if (!last) {
            LOGGER.error("NOT LAST OUCH!!!");
            throw new RuntimeException("OUCH");
        }
        final byte type = bytes[RpcConstants.TYPE_INDEX];
        final int id = BytesUtil.readInteger(bytes, RpcConstants.ID_INDEX);
        LOGGER.trace(sealer.getIdentity() + " received type: " + type + " id: " + id);

        synchronized (session) {

            if (type == RpcConstants.TYPE_HANDSHAKE_REQUEST) {
                if (!sealer.verifyHandshake(bytes)) {
                    LOGGER.warn("Invalid handshake from client.");
                    return;
                }
                session.getAsyncRemote().sendBinary(ByteBuffer.wrap(sealer.getHandshake(false)));
                return;
            } else if (type == RpcConstants.TYPE_HANDSHAKE_RESPONSE) {
                responseBytes = bytes;
                session.notifyAll();
                return;
            } else if (type == RpcConstants.TYPE_REQUEST || type == RpcConstants.TYPE_EVENT) {
                if (!sealer.verify(bytes)) {
                    LOGGER.warn("Request signature verification failed: " + session);
                    return;
                }

                final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes, RpcConstants.HEADER_LENGTH,
                        bytes.length - RpcConstants.HEADER_LENGTH);
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                try {
                    byte protocolVersion = RpcConstants.VERSION;
                    byte messageType = RpcConstants.TYPE_RESPONSE;
                    writerHeader(protocolVersion, messageType, id, outputStream);

                    server.setRethrowExceptions(true);
                    server.handle(inputStream, outputStream);

                } catch (final Exception e) {
                    LOGGER.error("JSON Request (" + type + ":" + id + "@" + sealer.getIdentity() + ") : " + new String(bytes, RpcConstants.HEADER_LENGTH,
                            bytes.length - RpcConstants.HEADER_LENGTH), e);
                }
                if (type == RpcConstants.TYPE_REQUEST) {
                    final byte[] responseBytes = outputStream.toByteArray();
                    sealer.seal(responseBytes);
                    LOGGER.trace(sealer.getIdentity() + " sent response: type: "
                            + responseBytes[RpcConstants.TYPE_INDEX] + " id: " + id);
                    session.getAsyncRemote().sendBinary(ByteBuffer.wrap(responseBytes));
                }
            } else if (type == RpcConstants.TYPE_RESPONSE && id == expectedId) {
                responseBytes = bytes;
                session.notifyAll();
                expectedId = -1;
            } else {
                if (!sealer.verify(bytes)) {
                    LOGGER.warn("Request signature verification failed: " + session);
                    return;
                }
                messageHandler.onMessage(type, id, bytes, RpcConstants.HEADER_LENGTH, bytes.length - RpcConstants.HEADER_LENGTH);
            }
        }
    }

    private void writerHeader(byte protocolVersion, byte messageType, int messageId, ByteArrayOutputStream outputStream) throws IOException {
        byte[] idBytes = new byte[4];
        BytesUtil.writeInteger(messageId, idBytes, 0);
        byte[] sealBytes = new byte[RpcConstants.SEAL_LENGTH];

        outputStream.write(protocolVersion);
        outputStream.write(messageType);
        outputStream.write(idBytes);
        outputStream.write(sealBytes);
    }
}