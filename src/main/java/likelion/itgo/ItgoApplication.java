package likelion.itgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ItgoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItgoApplication.class, args);
    }
}