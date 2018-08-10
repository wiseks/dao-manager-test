package com.manager.configration;

import org.springframework.beans.factory.FactoryBean;

public class ProxyBeanFactory<T> implements  FactoryBean<T>{

	private Class<T> mapperInterface;

	public Class<T> getMapperInterface() {
		return mapperInterface;
	}

	public void setMapperInterface(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	@Override
	public T getObject() throws Exception {
		return (T)new ProxyBean(mapperInterface).bind(mapperInterface);
	}

	@Override
	public Class<?> getObjectType() {
		return mapperInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
