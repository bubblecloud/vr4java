package org.bubblecloud.vr4java.rpc;

/**
 * Created by tlaukkan on 9/19/2014.
 */
public interface EchoService {
    @RpcMethod
    String echo(String message);

    @RpcEvent
    void event(String message);
}
