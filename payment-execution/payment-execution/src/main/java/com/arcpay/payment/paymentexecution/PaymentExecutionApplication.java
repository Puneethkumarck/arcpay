package com.arcpay.payment.paymentexecution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.arcpay.identity.client")
public class PaymentExecutionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentExecutionApplication.class, args);
    }
}
