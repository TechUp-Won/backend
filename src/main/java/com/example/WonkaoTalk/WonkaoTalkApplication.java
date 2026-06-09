package com.example.WonkaoTalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WonkaoTalkApplication {

	public static void main(String[] args) {
		SpringApplication.run(WonkaoTalkApplication.class, args);
	}

}
