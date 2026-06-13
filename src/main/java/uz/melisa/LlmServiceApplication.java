package uz.melisa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import uz.melisa.config.AiProperties;
import uz.melisa.config.ApplicationProperties;
import uz.melisa.config.ModelsProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableAspectJAutoProxy
@EnableConfigurationProperties({AiProperties.class, ApplicationProperties.class, ModelsProperties.class})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class LlmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmServiceApplication.class, args);
    }

}
