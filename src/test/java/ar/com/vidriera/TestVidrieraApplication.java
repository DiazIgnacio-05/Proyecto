package ar.com.vidriera;

import org.springframework.boot.SpringApplication;

public class TestVidrieraApplication {

	public static void main(String[] args) {
		SpringApplication.from(VidrieraApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
