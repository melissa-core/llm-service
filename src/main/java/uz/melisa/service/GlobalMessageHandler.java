package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.domain.ChatMemory;
import uz.melisa.dto.client.fitrat.FitratDetectLangResponseDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateRequestDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateResponseDTO;
import uz.melisa.dto.client.llama.LlamaChatResponseDTO;
import uz.melisa.dto.client.router.RateDifficultyResponseDTO;
import uz.melisa.dto.client.router.SummarizeMessageDTO;
import uz.melisa.dto.client.router.SummarizeResponseDTO;
import uz.melisa.dto.client.translator.TranslatorTextRequestDTO;
import uz.melisa.dto.client.translator.TranslatorTextResponseDTO;
import uz.melisa.repository.ChatMemoryRepository;
import uz.melisa.service.client.FitratServiceClient;
import uz.melisa.service.client.LlamaServiceClient;
import uz.melisa.service.client.RouterServiceClient;
import uz.melisa.service.client.TranslatorServiceClient;

import java.util.List;

import static uz.melisa.constants.LanguageConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalMessageHandler {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final ModelsProperties modelsProperties;
    private final RouterServiceClient routerServiceClient;
    private final LlamaServiceClient llamaServiceClient;
    private final FitratServiceClient fitratServiceClient;
    private final TranslatorServiceClient translatorServiceClient;
    private final ClaudeChatService claudeChatService;
    private final ChatMemoryRepository chatMemoryRepository;

    public String handleChatMessage(String message, String chatId) {
        String input = safeText(message);

        Long chatIdLong = parseChatIdLong(chatId);
        String prevSummary = loadPrevSummary(chatIdLong);

        RateDifficultyResponseDTO difficulty = routerServiceClient.rateDifficulty(input);
        if (!isEasy(difficulty)) {
            String modelInput = buildModelInput(prevSummary, input);
            String answer = safeText(claudeChatService.chatWithClaude(chatId, modelInput));
            saveSummary(chatIdLong, prevSummary, input, answer);
            return answer;
        }

        FitratDetectLangResponseDTO detected = fitratServiceClient.detect(input);
        String lang = detected != null ? safeText(detected.getLang()) : "";

        if (!isUzbek(lang)) {
            String modelInput = buildModelInput(prevSummary, input);
            String answer = llamaAnswer(modelInput);
            saveSummary(chatIdLong, prevSummary, input, answer);
            return answer;
        }

        boolean wasCyril = UZ_CYRIL.equals(lang);

        String modelInputUz = buildModelInput(prevSummary, input);

        String uzLatnInput = wasCyril ? transliterate(modelInputUz, TRANSLITERATE_CYRL, TRANSLITERATE_LATN) : modelInputUz;
        String enInput = translate(uzLatnInput, LANG_UZ, LANG_EN, SCRIPT_LATN);

        String enAnswer = llamaAnswer(enInput);

        String uzLatnAnswer = translate(enAnswer, LANG_EN, LANG_UZ, SCRIPT_LATN);
        String finalAnswer = wasCyril
                ? transliterate(uzLatnAnswer, TRANSLITERATE_LATN, TRANSLITERATE_CYRL)
                : uzLatnAnswer;

        saveSummary(chatIdLong, prevSummary, input, finalAnswer);
        return finalAnswer;
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

    private String loadPrevSummary(Long chatIdLong) {
        if (chatIdLong == null) return "";
        return chatMemoryRepository.findByChatId(chatIdLong)
                .map(ChatMemory::getSummary)
                .map(this::safeText)
                .orElse("");
    }

    private void saveSummary(Long chatIdLong, String prevSummary, String userText, String assistantText) {
        if (chatIdLong == null) return;

        List<SummarizeMessageDTO> msgs = List.of(
                new SummarizeMessageDTO(ROLE_USER, safeText(userText)),
                new SummarizeMessageDTO(ROLE_ASSISTANT, safeText(assistantText))
        );

        SummarizeResponseDTO resp = routerServiceClient.summarize(msgs, safeText(prevSummary));
        if (resp == null || safeText(resp.getSummary()).isEmpty()) return;

        ChatMemory mem = chatMemoryRepository.findByChatId(chatIdLong)
                .orElseGet(() -> ChatMemory.builder().chatId(chatIdLong).build());

        mem.setSummary(resp.getSummary());
        chatMemoryRepository.save(mem);
    }

    private String buildModelInput(String prevSummary, String userText) {
        String s = safeText(prevSummary);
        String u = safeText(userText);
        if (s.isEmpty()) return u;
        return "Summary:\n" + s + "\n\nUser:\n" + u;
    }

    private Long parseChatIdLong(String chatId) {
        String s = safeText(chatId);
        if (s.isEmpty()) return null;
        String digits = s.replaceAll("\\D+", "");
        if (digits.isEmpty()) return null;
        try {
            return Long.parseLong(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeText(String s) {
        return s == null ? "" : s.trim();
    }
}
