package com.manager.dao;

import com.manager.annotations.ShiroDescription;

public interface UserDao {

	@ShiroDescription(name="test")
	public void test();
}
