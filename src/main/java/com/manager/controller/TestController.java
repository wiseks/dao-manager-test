package com.manager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.manager.service.TestService;

@Controller
public class TestController {

	@Autowired
	private TestService testSerivce;
	
	@ResponseBody
	@RequestMapping("/test")
	public String test(){
		testSerivce.test();
		return "sucess";
	}
}
