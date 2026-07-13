package io.github.ivannavas.local_server_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LocalServerManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocalServerManagementApplication.class, args);
	}

}
