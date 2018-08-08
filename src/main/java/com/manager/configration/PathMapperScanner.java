package com.manager.configration;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

public class PathMapperScanner extends ClassPathBeanDefinitionScanner {

	private DaoFactoryBean<?> mapperFactoryBean = new DaoFactoryBean<>();
	
	public PathMapperScanner(BeanDefinitionRegistry registry) {
		super(registry);
	}
	
	@Override
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

	    if (beanDefinitions.isEmpty()) {
	      logger.warn("No MyBatis mapper was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
	    } else {
	      processBeanDefinitions(beanDefinitions);
	    }

	    return beanDefinitions;
	}
	
	private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
	    GenericBeanDefinition definition;
	    for (BeanDefinitionHolder holder : beanDefinitions) {
	      definition = (GenericBeanDefinition) holder.getBeanDefinition();

	      if (logger.isDebugEnabled()) {
	        logger.debug("Creating MapperFactoryBean with name '" + holder.getBeanName() 
	          + "' and '" + definition.getBeanClassName() + "' mapperInterface");
	      }

	      // the mapper interface is the original class of the bean
	      // but, the actual class of the bean is MapperFactoryBean
	      definition.getConstructorArgumentValues().addGenericArgumentValue(definition.getBeanClassName()); // issue #59
	      definition.setBeanClass(this.mapperFactoryBean.getClass());

	      definition.getPropertyValues().add("addToConfig", true);

	      boolean explicitFactoryUsed = false;


	      if (!explicitFactoryUsed) {
	        if (logger.isDebugEnabled()) {
	          logger.debug("Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
	        }
	        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
	      }
	    }
	  }

}
