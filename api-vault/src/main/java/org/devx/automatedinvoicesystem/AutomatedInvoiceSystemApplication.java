package org.devx.automatedinvoicesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AutomatedInvoiceSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomatedInvoiceSystemApplication.class, args);
		System.out.println("Automated Invoice System is running...");
	}

}
