package uz.melisa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "melisa.ai")
public class AiProperties {

    private Memory memory;
    private ModelProfile classifier;
    private ModelProfile general;
    private ModelProfile product;
    private ModelProfile summary;

    @Getter
    @Setter
    public static class Memory {
        private long maxMessages;
        private long ttlSeconds;
        private int maxL2Summaries = 3;
    }

    @Getter
    @Setter
    public static class ModelProfile {
        private String model;
        private Integer maxOutputTokens;
        private Integer maxInputChars;
        private Double temperature;
        private Double topP;
    }
}