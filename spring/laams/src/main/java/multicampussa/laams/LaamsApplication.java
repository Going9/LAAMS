package multicampussa.laams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EnableMongoRepositories
public class LaamsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LaamsApplication.class, args);
	}

}
