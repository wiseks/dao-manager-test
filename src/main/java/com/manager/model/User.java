package com.manager.model;

import com.manager.persistence.annotation.Column;
import com.manager.persistence.annotation.Pk;
import com.manager.persistence.annotation.Table;

@Table(name="user")
public class User {

	@Pk
	@Column(name="id",comment="id")
	private int id;
	
	@Column(name="name",comment="name")
	private String name;
	
	@Column(name="desc",comment="desc")
	private String desc;

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}
	
	
}
