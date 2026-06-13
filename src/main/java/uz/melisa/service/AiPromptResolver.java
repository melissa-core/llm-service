package uz.melisa.service;

import org.springframework.stereotype.Component;
import uz.melisa.config.AiSystemPrompts;
import uz.melisa.enums.AiRoute;

@Component
public class AiPromptResolver {

    public String resolve(AiRoute route) {
        return switch (route) {
            case CLASSIFIER -> AiSystemPrompts.CLASSIFIER_SYSTEM;
            case GENERAL -> AiSystemPrompts.GENERAL_SYSTEM;
            case PRODUCT -> AiSystemPrompts.PRODUCT_SYSTEM;
            case SUMMARY -> AiSystemPrompts.SUMMARY_SYSTEM;
        };
    }
}
