package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.domain.ChatMemory;
import uz.melisa.dto.chat.ChatUserMessageDTO;
import uz.melisa.dto.claude.ClaudeResult;
import uz.melisa.dto.client.catalog.EmbeddingProductSearchRequestDTO;
import uz.melisa.dto.client.catalog.ProductDTO;
import uz.melisa.dto.client.embedding.VoyageEmbeddingResponseDTO;
import uz.melisa.dto.client.fitrat.FitratDetectLangResponseDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateRequestDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateResponseDTO;
import uz.melisa.dto.client.llama.LlamaChatResponseDTO;
import uz.melisa.dto.client.router.FoodIntentResponseDTO;
import uz.melisa.dto.client.router.RateDifficultyResponseDTO;
import uz.melisa.dto.client.translator.TranslatorTextRequestDTO;
import uz.melisa.dto.client.translator.TranslatorTextResponseDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.repository.ChatMemoryRepository;
import uz.melisa.service.client.*;
import uz.melisa.service.impl.ChatPostProcessService;
import uz.melisa.util.StringUtil;

import java.util.List;
import java.util.Map;

import static uz.melisa.constants.LanguageConstants.*;
import static uz.melisa.util.LangUtil.currentLang;
import static uz.melisa.util.SecurityUtil.getCurrentUserId;
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
    private final EmbeddingClient embeddingClient;
    private final EmbeddingService embeddingService;
    private final CatalogServiceClient catalogServiceClient;
    private final MessageProductSuggestionService messageProductSuggestionService;

    public Map<Boolean, String> handleChatMessage(ChatUserMessageDTO chatUserMessage) {
        Long userId = getCurrentUserId();
        Long chatId = chatUserMessage.getChatId();
        Long messageId = chatUserMessage.getMessageId();
        String input = trimToEmpty(chatUserMessage.getMessage());
        String conversationKey = "chat:" + chatId;
        String prevSummary = loadPrevSummary(chatId);

        FitratDetectLangResponseDTO detected = tryDetectLang(input);
        String requestLang = detected == null ? null : trimToEmpty(detected.getLang());
        String requestScript = resolveScript(requestLang);

        FoodIntentResponseDTO isFoodIntent = routerServiceClient.isFoodIntent(input);
        if (validateFoodIntent(isFoodIntent)) {
            VoyageEmbeddingResponseDTO embedded = performVoyageEmbedding(input, messageId);
            if (embedded.getData() != null && !embedded.getData().isEmpty()) {
                CommonResponse<List<ProductDTO>> embeddingToProduct = catalogServiceClient.embeddingToProduct(userId,
                        buildEmbeddingProductSearchRequest(embedded)
                );
                List<ProductDTO> products = (embeddingToProduct != null && embeddingToProduct.getData() != null) ? embeddingToProduct.getData() : List.of();
                messageProductSuggestionService.saveProductSuggestion(messageId,
                        products.stream().map(ProductDTO::getId).toList());
                if (products.size() > 5) {
                    products = products.subList(0, 5);
                }
                String modelInput = buildModelInput(prevSummary, input, products);
                ClaudeResult result = claudeChatService.chatWithClaude(conversationKey, modelInput);
                String answer = result == null ? "" : trimToEmpty(result.getText());
                chatPostProcessService.processClaude(
                        chatId, messageId, conversationKey, input, answer,
                        isFoodIntent.getIsFood(), requestLang, requestScript, modelInput, result
                );
                chatPostProcessService.processLangDetection(detected, chatId, messageId,
                        requestLang, requestScript, MessageAuthorityType.USER,
                        (detected == null || detected.getRequestText() == null) ? "" : trimToEmpty(detected.getRequestText()));
                return Map.of(true, answer);
            }
        }

        if (!isUzbek(requestLang)) {
            LlamaChatResponseDTO llamaResp = llamaServiceClient.chat(input);
            String answer = extractLlamaText(llamaResp);

            chatPostProcessService.processLlama(
                    chatId, messageId, conversationKey, input, answer,
                    isFoodIntent.getIsFood(), requestLang, requestScript, llamaResp
            );

            chatPostProcessService.processLangDetection(detected, chatId, messageId,
                    requestLang, requestScript, MessageAuthorityType.USER,
                    (detected == null || detected.getRequestText() == null) ? "" : trimToEmpty(detected.getRequestText()));
            return Map.of(false, answer);
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
                chatId, messageId, conversationKey, input, trimToEmpty(finalAnswer),
                isFoodIntent.getIsFood(), requestLang, requestScript, llamaResp
        );

        chatPostProcessService.processLangDetection(detected, chatId, messageId,
                requestLang, requestScript, MessageAuthorityType.USER,
                (detected == null || detected.getRequestText() == null) ? "" : trimToEmpty(detected.getRequestText()));
        return Map.of(false, trimToEmpty(finalAnswer));
    }

    private EmbeddingProductSearchRequestDTO buildEmbeddingProductSearchRequest(VoyageEmbeddingResponseDTO embedded) {
        return new EmbeddingProductSearchRequestDTO(
                toFloatArray(embedded.getData().getFirst().getEmbedding()), 50, currentLang()
        );
    }

    private VoyageEmbeddingResponseDTO performVoyageEmbedding(String input, Long messageId) {
        VoyageEmbeddingResponseDTO embedded = embeddingClient.embed(input);
        embeddingService.saveEmbedding(messageId, embedded, input);
        return embedded;
    }

    private FitratDetectLangResponseDTO tryDetectLang(String text) {
        try {
            return fitratServiceClient.detect(text);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean validateFoodIntent(FoodIntentResponseDTO foodIntentResponseDTO) {
        if (foodIntentResponseDTO == null || foodIntentResponseDTO.getIsFood() == null) return false;

        return foodIntentResponseDTO.getIsFood();
    }

    private boolean isEasy(RateDifficultyResponseDTO dto) {
        if (dto == null) return false;
        return dto.getDifficulty() <= modelsProperties.getDifficultyRate();
    }

    private static float[] toFloatArray(List<Float> list) {
        if (list == null || list.isEmpty()) return new float[0];
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Float v = list.get(i);
            arr[i] = (v == null) ? 0f : v;
        }
        return arr;
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

        if (resp == null || resp.getTranslations() == null || resp.getTranslations().isEmpty()) {
            return "";
        }
        return trimToEmpty(resp.getTranslations().getFirst());
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

    private String buildModelInput(String prevSummary, String userText, List<ProductDTO> products) {
        String summary = trimToEmpty(prevSummary);
        String user = trimToEmpty(userText);

        StringBuilder sb = new StringBuilder(1024);

        if (!summary.isEmpty()) {
            sb.append("Summary:\n").append(summary).append("\n\n");
        }

        sb.append("User:\n").append(user).append("\n\n");

        sb.append("CandidateProducts (max 5, use ONLY these for recommendations):\n");

        if (products == null || products.isEmpty()) {
            sb.append("[NONE]\n");
        } else {
            for (int i = 0; i < products.size(); i++) {
                ProductDTO p = products.get(i);

                sb.append(i + 1).append(") ")
                        .append(safe(p.getName()))
                        .append(" | price=").append(p.getPrice() == null ? "N/A" : p.getPrice()).append(" UZS")
                        .append(" | category=").append(p.getCategory() == null ? "N/A" : p.getCategory())
                        .append(" | tags=").append(p.getTags() == null ? "N/A" : p.getTags())
                        .append("\n")
                        .append("   desc: ").append(truncate(safe(p.getDescription()), 180))
                        .append("\n");
            }
        }

        sb.append("\nInstructions:\n")
                .append("- If CandidateProducts is [NONE], do NOT invent products. Give general advice and ask 1 short follow-up question.\n")
                .append("- If products exist, rank them best->worst for the user’s request and explain briefly why.\n")
                .append("- Do not mention databases, embeddings, vectors, or internal systems.\n");

        return sb.toString();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }
}
