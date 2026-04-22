package com.wastecoder.picpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PicpaySimplificadoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PicpaySimplificadoApplication.class, args);
	}

}
