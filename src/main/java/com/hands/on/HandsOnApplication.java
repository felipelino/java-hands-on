package com.hands.on;

import com.hands.on.stream.Topics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;

@EnableBinding(Topics.class)
@SpringBootApplication
public class HandsOnApplication {
	public static void main(String[] args) {
		SpringApplication.run(HandsOnApplication.class, args);
	}
}
