package uz.melisa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import uz.melisa.config.ApplicationProperties;
import uz.melisa.config.ClaudeProperties;

@SpringBootApplication
@EnableConfigurationProperties({ClaudeProperties.class, ApplicationProperties.class})
public class LlmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmServiceApplication.class, args);
    }

}
