package com.manager.configration;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

public class PathMapperScanner extends ClassPathBeanDefinitionScanner {

	private TestFactoryBean<?> mapperFactoryBean = new TestFactoryBean<Object>();
	
	public PathMapperScanner(BeanDefinitionRegistry registry) {
		super(registry);
	}

}
