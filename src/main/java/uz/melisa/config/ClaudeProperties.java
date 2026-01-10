package uz.melisa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "melisa.ai.claude")
public class ClaudeProperties {

    private String system;
    private String model;
    private Integer maxOutputTokens;
    private Integer maxInputChars;
    private Double temperature;
    private Double topP;
}



