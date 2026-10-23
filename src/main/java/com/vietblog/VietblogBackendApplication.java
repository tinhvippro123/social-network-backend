package com.vietblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VietblogBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(VietblogBackendApplication.class, args);
	}

}

