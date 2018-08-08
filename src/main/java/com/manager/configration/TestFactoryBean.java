package com.manager.configration;

import org.springframework.beans.factory.FactoryBean;

public class TestFactoryBean<T> implements FactoryBean<T> {

	private Class<T> mapperInterface;
	
	public TestFactoryBean() {
	}

	public TestFactoryBean(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	@Override
	public T getObject() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getObjectType() {
		return this.mapperInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
