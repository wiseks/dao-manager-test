package com.manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.manager.dao.UserDao;

@Service
public class TestService {

	@Autowired
	private UserDao userDao;
	
	public void test(){
		userDao.test();
	}
}
