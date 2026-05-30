package com.arcpay.payment.paymentexecution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PaymentExecutionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentExecutionApplication.class, args);
    }
}
