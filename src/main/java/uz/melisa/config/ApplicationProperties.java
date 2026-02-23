package uz.melisa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private Downstream downstream;

    @Getter
    @Setter
    public static class Downstream {
        private String hmacSecret;
        private String catalogServiceUrl;
    }
}
