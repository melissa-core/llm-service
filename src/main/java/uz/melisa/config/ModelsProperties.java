package uz.melisa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "melisa.models")
public class ModelsProperties {

    private double difficultyRate;
    private RouterModel routerModel;
    private LlamaModel llamaModel;
    private TranslatorModel translatorModel;
    private FitratModel fitratModel;

    @Getter
    @Setter
    public static class RouterModel {
        private String baseUrl;
        private String authKey;

        private String difficultyPath;
        private String summarizePath;

        private Integer maxTokens;
    }

    @Getter
    @Setter
    public static class LlamaModel {
        private String baseUrl;
        private String authKey;

        private String chatPath;
        private String model;

        private Integer maxTokens;
        private Double temperature;
    }

    @Getter
    @Setter
    public static class TranslatorModel {
        private String baseUrl;
        private String authKey;
        private String translatorPath;
    }

    @Getter
    @Setter
    public static class FitratModel {
        private String baseUrl;
        private String authKey;

        private String normalizePath;
        private String transliteratePath;
        private String tokenizePath;
        private String detectPath;
        private String healthzPath;
    }
}
