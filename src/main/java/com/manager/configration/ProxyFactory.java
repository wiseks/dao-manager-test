package com.manager.configration;

import java.lang.reflect.Proxy;

public class ProxyFactory<T> {

	private final Class<T> mapperInterface;

	public ProxyFactory(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	@SuppressWarnings("unchecked")
	protected T newInstance(ProxyBean<T> mapperProxy) {
		return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface },
				mapperProxy);
	}

	public T newInstance() {
		final ProxyBean<T> mapperProxy = new ProxyBean<T>(mapperInterface);
		return newInstance(mapperProxy);
	}

}
