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

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for create client proxies.
 */
public abstract class RpcProxyUtil {

	private static final Logger LOGGER = Logger.getLogger(RpcProxyUtil.class.getName());

	/**
	 * Creates a composite service using all of the given
	 * services.
	 * 
	 * @param classLoader the {@link ClassLoader}
	 * @param services the service objects
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(
		ClassLoader classLoader, Object[] services, boolean allowMultipleInheritance) {
		return createCompositeServiceProxy(classLoader, services, null, allowMultipleInheritance);
	}

	/**
	 * Creates a composite service using all of the given
	 * services and implementing the given interfaces.
	 * 
	 * @param classLoader the {@link ClassLoader}
	 * @param services the service objects
	 * @param serviceInterfaces the service interfaces
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(
		ClassLoader classLoader, Object[] services,
		Class<?>[] serviceInterfaces, boolean allowMultipleInheritance) {
		
		// get interfaces
		Set<Class<?>> interfaces = new HashSet<Class<?>>();
		if (serviceInterfaces!=null) {
			interfaces.addAll(Arrays.asList(serviceInterfaces));
		} else {
			for (Object o : services) {
				interfaces.addAll(Arrays.asList(o.getClass().getInterfaces()));
			}
		}

		// build the service map
		final Map<Class<?>, Object> serviceMap = new HashMap<Class<?>, Object>();
		for (Class<?> clazz : interfaces) {

			// we will allow for this, but the first
			// object that was registered wins
			if (serviceMap.containsKey(clazz) && allowMultipleInheritance) {
				continue;
			} else if (serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException(
					"Multiple inheritance not allowed "+clazz.getName());
			}

			// find a service for this interface
			for (Object o : services) {
				if (clazz.isInstance(o)) {
					if (LOGGER.isLoggable(Level.FINE)) {
						LOGGER.fine("Using "+o.getClass().getName()+" for "+clazz.getName());
					}
					serviceMap.put(clazz, o);
					break;
				}
			}

			// make sure we have one
			if (!serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException(
					"None of the provided services implement "+clazz.getName());
			}
		}

		// now create the proxy
		return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class<?>[0]),
			new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
				Class<?> clazz = method.getDeclaringClass();
				if (clazz == Object.class) {
					return proxyObjectMethods(method, proxy, args);
				}
				return method.invoke(serviceMap.get(clazz), args);
			}
		});
	}

	/**
	 * Creates a client proxy.
     *
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the client
	 * @return the proxied interface
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final boolean useNamedParams,
		final RpcEndpoint client) {

		// create and return the proxy
		return (T)Proxy.newProxyInstance(
			classLoader,
			new Class<?>[] {proxyInterface},
			new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
					if (method.getDeclaringClass() == Object.class) {
						return proxyObjectMethods(method, proxy, args);
					}
					Object arguments = ReflectionUtil.parseArguments(method, args, useNamedParams);

					String methodName=method.getName();
					JsonRpcMethod methodAnnotation = method.getAnnotation(JsonRpcMethod.class);
		            if (methodAnnotation!=null && methodAnnotation.value()!=null) {
		            	methodName=methodAnnotation.value();
		            }

                    final RpcEvent rpcEvent = method.getAnnotation(RpcEvent.class);

                    final RpcMethod rpcMethod = method.getAnnotation(RpcMethod.class);

                    if (rpcEvent == null && rpcMethod == null) {
                        throw new RuntimeException("Not an RPC event or method.");
                    }

					return client.invoke(
                            methodName, arguments, method.getGenericReturnType(), rpcEvent != null);
				}
			});
	}

	/**
	 * Creates a client proxy.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the client
	 * @return the proxied interface
	 */
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final RpcEndpoint client) {
		return createClientProxy(classLoader, proxyInterface, false, client);
	}

	private static Object proxyObjectMethods(Method method, Object proxyObject, Object[] args) {
		String name = method.getName();
		if (name.equals("toString")) {
			return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
		}
		if (name.equals("hashCode")) {
			return System.identityHashCode(proxyObject);
		}
		if (name.equals("equals")) {
			return proxyObject == args[0];
		}
		throw new RuntimeException(method.getName() + " is not a member of java.lang.Object");
	}
}