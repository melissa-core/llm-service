package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.fitrat.FitratDetectLangResponseDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateRequestDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateResponseDTO;
import uz.melisa.dto.client.llama.LlamaChatResponseDTO;
import uz.melisa.dto.client.router.RateDifficultyResponseDTO;
import uz.melisa.dto.client.translator.TranslatorTextRequestDTO;
import uz.melisa.dto.client.translator.TranslatorTextResponseDTO;
import uz.melisa.service.client.FitratServiceClient;
import uz.melisa.service.client.LlamaServiceClient;
import uz.melisa.service.client.RouterServiceClient;
import uz.melisa.service.client.TranslatorServiceClient;

import static uz.melisa.constants.LanguageConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalMessageHandler {

    private final ModelsProperties modelsProperties;
    private final RouterServiceClient routerServiceClient;
    private final LlamaServiceClient llamaServiceClient;
    private final FitratServiceClient fitratServiceClient;
    private final TranslatorServiceClient translatorServiceClient;
    private final ClaudeChatService claudeChatService;

    public String handleChatMessage(String message, String chatId) {
        String input = safeText(message);

        RateDifficultyResponseDTO difficulty = routerServiceClient.rateDifficulty(input);
        if (!isEasy(difficulty)) {
            return safeText(claudeChatService.chatWithClaude(chatId, input));
        }

        FitratDetectLangResponseDTO detected = fitratServiceClient.detect(input);
        String lang = detected != null ? safeText(detected.getLang()) : "";

        if (!isUzbek(lang)) {
            return llamaAnswer(input);
        }

        boolean wasCyril = UZ_CYRIL.equals(lang);

        String uzLatnInput = wasCyril ? transliterate(input, TRANSLITERATE_CYRL, TRANSLITERATE_LATN) : input;
        String enInput = translate(uzLatnInput, LANG_UZ, LANG_EN, SCRIPT_LATN);

        String enAnswer = llamaAnswer(enInput);

        String uzLatnAnswer = translate(enAnswer, LANG_EN, LANG_UZ, SCRIPT_LATN);
        return wasCyril ? transliterate(uzLatnAnswer, TRANSLITERATE_LATN, TRANSLITERATE_CYRL) : uzLatnAnswer;
    }

    private boolean isEasy(RateDifficultyResponseDTO dto) {
        if (dto == null) return false;
        return dto.getDifficulty() <= modelsProperties.getDifficultyRate();
    }

    private boolean isUzbek(String lang) {
        return UZ_LATN.equals(lang) || UZ_CYRIL.equals(lang);
    }

    private String llamaAnswer(String text) {
        LlamaChatResponseDTO resp = llamaServiceClient.chat(text);
        if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()) return "";
        var choice = resp.getChoices().get(0);
        if (choice == null || choice.getMessage() == null) return "";
        return safeText(choice.getMessage().getContent());
    }

    private String translate(String text, String source, String target, String script) {
        TranslatorTextRequestDTO req = new TranslatorTextRequestDTO(text, source, target, script);
        TranslatorTextResponseDTO resp = translatorServiceClient.translate(req);
        return resp == null ? "" : safeText(resp.getText());
    }

    private String transliterate(String text, String from, String to) {
        FitratTransliterateRequestDTO req = new FitratTransliterateRequestDTO(text, from, to);
        FitratTransliterateResponseDTO resp = fitratServiceClient.transliterate(req);
        return resp == null ? "" : safeText(resp.getText());
    }

    private String safeText(String s) {
        return s == null ? "" : s.trim();
    }
}
