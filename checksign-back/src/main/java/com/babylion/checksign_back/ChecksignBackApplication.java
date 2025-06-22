package com.babylion.checksign_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ChecksignBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChecksignBackApplication.class, args);
	}

}
