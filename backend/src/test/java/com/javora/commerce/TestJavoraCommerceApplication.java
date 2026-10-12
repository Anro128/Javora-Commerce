package com.javora.commerce;

import org.springframework.boot.SpringApplication;

public class TestJavoraCommerceApplication {

	public static void main(String[] args) {
		SpringApplication.from(JavoraCommerceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
