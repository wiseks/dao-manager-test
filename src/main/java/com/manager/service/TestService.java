package com.manager.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.manager.dao.UserDao;
import com.manager.persistence.db.JDBCTableGenerator;

@Service
public class TestService {

	@Autowired
	private UserDao userDao;
	
	@Autowired
	private JDBCTableGenerator jd;
	
	public void test(){
		userDao.test();
	}
	
	@PostConstruct
	private void init(){
		jd.fixtTable("com.manager");
	}
}
