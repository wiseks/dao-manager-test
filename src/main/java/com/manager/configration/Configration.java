package com.manager.configration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Configration {

	@Bean
	public ScannerConfigurer scannerConfigurer() {
		String packageName = "com.manager.dao";
		ScannerConfigurer config = new ScannerConfigurer(packageName);
		
		return config;
	}
	

}
