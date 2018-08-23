package com.manager.service;

import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.manager.configration.ClassResolverUtil;
import com.manager.dao.UserDao;
import com.manager.persistence.annotation.Table;
import com.manager.persistence.db.JDBCRepository;
import com.manager.persistence.db.TableMeta;

@Service
public class TestService {

	@Autowired
	private UserDao userDao;
	
	@Autowired
	private JDBCRepository jd;
	
	public void test(){
		userDao.test();
	}
	
	@PostConstruct
	private void init(){
		ClassResolverUtil util = new ClassResolverUtil();
		Set<Class<?>> set = util.find("com.manager").getMatches();
		for(Class<?> clazz : set){
			Table table = clazz.getAnnotation(Table.class);
			if(table!=null){
				TableMeta tm = TableMeta.parse(clazz);
				try {
					jd.fixTable(clazz, tm);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}
