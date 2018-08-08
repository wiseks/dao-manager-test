package com.manager.configration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Configration {

	@Bean
	public ScannerConfigurer scannerConfigurer(){
		ScannerConfigurer config = new ScannerConfigurer("com.manager");
		return config;
	}
}
