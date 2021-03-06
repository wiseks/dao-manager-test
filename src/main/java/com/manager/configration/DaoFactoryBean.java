package com.manager.configration;

import org.springframework.beans.factory.FactoryBean;

public class DaoFactoryBean<T>  implements FactoryBean<T> {

	public DaoFactoryBean() {
		
	}

	private Class<T> mapperInterface;
	
	public DaoFactoryBean(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	@Override
	public T getObject() throws Exception {
		return null;//ProxyBeanCreator.getMapper(mapperInterface.getName());
	}

	@Override
	public Class<?> getObjectType() {
		return Object.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
