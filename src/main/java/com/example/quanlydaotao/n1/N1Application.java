package com.example.quanlydaotao.n1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = "com.example.quanlydaotao.n1")
@EnableJpaAuditing
public class N1Application {

	public static void main(String[] args) {
		SpringApplication.run(N1Application.class, args);
	}

}

