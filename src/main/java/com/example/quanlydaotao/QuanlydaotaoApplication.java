package com.example.quanlydaotao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class QuanlydaotaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuanlydaotaoApplication.class, args);
	}

}
