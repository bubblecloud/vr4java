package org.bubblecloud.vr4java.rpc;

import junit.framework.Assert;
import org.bouncycastle.util.encoders.Hex;

/**
 * Created by tlaukkan on 9/20/14.
 */
public class EchoServiceImpl implements EchoService, MessageHandler {

    private final String responsePrefix;

    private static int eventCount = 0;

    private static int messageCount = 0;

    public EchoServiceImpl(final String responsePrefix) {
        this.responsePrefix = responsePrefix;
    }


    public static int getEventCount() {
        return eventCount;
    }

    public static int getMessageCount() {
        return messageCount;
    }

    @Override
    public String echo(final String message) {
        return responsePrefix + ":" + message;
    }
    @Override
    public void event(String message) {
        Assert.assertEquals("event-test", message);
        eventCount++;
    }

    @Override
    public void onMessage(byte messageType, int messageId, byte[] buffer, int startIndex, int length) {
        Assert.assertEquals((byte) 20, messageType);
        Assert.assertEquals("message-test", new String(buffer, startIndex, length));
        messageCount++;
    }
}
