package org.bubblecloud.vr4java.api;

import org.bubblecloud.vr4java.rpc.RpcMethod;

import java.util.List;

/**
 * Created by tlaukkan on 10/25/2014.
 */
public interface PackageService {

    /**
     * Gets asset package IDs.
     * @return IDs of asset packages
     */
    @RpcMethod
    public List<String> getPackageIds();

}
