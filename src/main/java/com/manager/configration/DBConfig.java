package com.manager.configration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.manager.persistence.db.DB;
import com.manager.persistence.db.JDBCTableGenerator;
import com.yaowan.game.common.util.PropertiesLoader;

@Configuration
public class DBConfig {

	@Bean
	public DB db() throws Exception{
		DB db = new DB(PropertiesLoader.load("db.properties"));
		return db;
	}
	
	@Bean
	public JDBCTableGenerator jDBCRepository(){
		try {
			JDBCTableGenerator jd = new JDBCTableGenerator(db());
			return jd;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
