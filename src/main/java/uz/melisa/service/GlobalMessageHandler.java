package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.domain.ChatMemory;
import uz.melisa.dto.chat.ChatUserMessageDTO;
import uz.melisa.dto.claude.ClaudeResult;
import uz.melisa.dto.client.fitrat.FitratDetectLangResponseDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateRequestDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateResponseDTO;
import uz.melisa.dto.client.llama.LlamaChatResponseDTO;
import uz.melisa.dto.client.router.RateDifficultyResponseDTO;
import uz.melisa.dto.client.translator.TranslatorTextRequestDTO;
import uz.melisa.dto.client.translator.TranslatorTextResponseDTO;
import uz.melisa.repository.ChatMemoryRepository;
import uz.melisa.service.client.FitratServiceClient;
import uz.melisa.service.client.LlamaServiceClient;
import uz.melisa.service.client.RouterServiceClient;
import uz.melisa.service.client.TranslatorServiceClient;
import uz.melisa.service.impl.ChatPostProcessService;
import uz.melisa.util.StringUtil;

import static uz.melisa.constants.LanguageConstants.*;
import static uz.melisa.util.StringUtil.trimToEmpty;

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
    private final ChatMemoryRepository chatMemoryRepository;
    private final ChatPostProcessService chatPostProcessService;

    public String handleChatMessage(ChatUserMessageDTO chatUserMessage) {
        Long chatId = chatUserMessage.getChatId();
        String input = trimToEmpty(chatUserMessage.getMessage());
        String conversationKey = "chat:" + chatId;
        String prevSummary = loadPrevSummary(chatId);

        FitratDetectLangResponseDTO detected = tryDetectLang(input);
        String requestLang = detected == null ? null : trimToEmpty(detected.getLang());
        String requestScript = resolveScript(requestLang);

        RateDifficultyResponseDTO difficultyDto = routerServiceClient.rateDifficulty(input);
        Double difficulty = difficultyDto == null ? null : difficultyDto.getDifficulty();

        if (!isEasy(difficultyDto)) {
            String modelInput = buildModelInput(prevSummary, input);
            ClaudeResult result = claudeChatService.chatWithClaude(conversationKey, modelInput);
            String answer = result == null ? "" : trimToEmpty(result.getText());

            chatPostProcessService.processClaude(
                    chatId, conversationKey, input, answer,
                    difficulty, requestLang, requestScript, result
            );

            return answer;
        }

        if (!isUzbek(requestLang)) {
            LlamaChatResponseDTO llamaResp = llamaServiceClient.chat(input);
            String answer = extractLlamaText(llamaResp);

            chatPostProcessService.processLlama(
                    chatId, conversationKey, input, answer,
                    difficulty, requestLang, requestScript, llamaResp
            );

            return answer;
        }

        boolean wasCyril = UZ_CYRIL.equals(requestLang);

        String modelInputUz = buildModelInput(prevSummary, input);
        String uzLatnInput = wasCyril ? transliterate(modelInputUz, TRANSLITERATE_CYRL, TRANSLITERATE_LATN) : modelInputUz;
        String enInput = translate(uzLatnInput, LANG_UZ, LANG_EN, SCRIPT_LATN);

        LlamaChatResponseDTO llamaResp = llamaServiceClient.chat(enInput);
        String enAnswer = extractLlamaText(llamaResp);

        String uzLatnAnswer = translate(enAnswer, LANG_EN, LANG_UZ, SCRIPT_LATN);
        String finalAnswer = wasCyril ? transliterate(uzLatnAnswer, TRANSLITERATE_LATN, TRANSLITERATE_CYRL) : uzLatnAnswer;

        chatPostProcessService.processLlama(
                chatId, conversationKey, input, trimToEmpty(finalAnswer),
                difficulty, requestLang, requestScript, llamaResp
        );

        return trimToEmpty(finalAnswer);
    }

    private FitratDetectLangResponseDTO tryDetectLang(String text) {
        try {
            return fitratServiceClient.detect(text);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isEasy(RateDifficultyResponseDTO dto) {
        if (dto == null) return false;
        return dto.getDifficulty() <= modelsProperties.getDifficultyRate();
    }

    private boolean isUzbek(String lang) {
        return UZ_LATN.equals(lang) || UZ_CYRIL.equals(lang);
    }

    private String resolveScript(String lang) {
        if (UZ_CYRIL.equals(lang)) return "Cyrl";
        if (UZ_LATN.equals(lang)) return "Latn";
        return null;
    }

    private String extractLlamaText(LlamaChatResponseDTO resp) {
        if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()) return "";
        var choice = resp.getChoices().getFirst();
        if (choice == null || choice.getMessage() == null) return "";
        return trimToEmpty(choice.getMessage().getContent());
    }

    private String translate(String text, String source, String target, String script) {
        TranslatorTextRequestDTO req = new TranslatorTextRequestDTO(text, source, target, script);
        TranslatorTextResponseDTO resp = translatorServiceClient.translate(req);
        return resp == null ? "" : trimToEmpty(resp.getText());
    }

    private String transliterate(String text, String from, String to) {
        FitratTransliterateRequestDTO req = new FitratTransliterateRequestDTO(text, from, to);
        FitratTransliterateResponseDTO resp = fitratServiceClient.transliterate(req);
        return resp == null ? "" : trimToEmpty(resp.getText());
    }

    private String loadPrevSummary(Long chatId) {
        if (chatId == null) return "";
        return chatMemoryRepository.findByChatId(chatId)
                .map(ChatMemory::getSummary)
                .map(StringUtil::trimToEmpty)
                .orElse("");
    }

    private String buildModelInput(String prevSummary, String userText) {
        String summary = trimToEmpty(prevSummary);
        String user = trimToEmpty(userText);
        if (summary.isEmpty()) return user;
        return "Summary:\n" + summary + "\n\nUser:\n" + user;
    }
}
