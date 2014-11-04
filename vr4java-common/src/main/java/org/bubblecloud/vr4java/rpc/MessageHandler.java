package org.bubblecloud.vr4java.rpc;

/**
 * Created by tlaukkan on 9/21/14.
 */
public interface MessageHandler {
    void onMessage(final byte messageType, final int messageId, final byte[] buffer, final int startIndex, final int length);
}
