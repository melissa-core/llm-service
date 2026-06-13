package uz.melisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.domain.ChatMemory;
import uz.melisa.dto.chat.ChatUserMessageDTO;
import uz.melisa.dto.claude.AIResult;
import uz.melisa.dto.client.catalog.EmbeddingProductSearchRequestDTO;
import uz.melisa.dto.client.catalog.ProductDTO;
import uz.melisa.dto.client.embedding.OpenAiEmbeddingResponseDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.ChatStreamPlan;
import uz.melisa.dto.message.ProductBasedMessage;
import uz.melisa.enums.AiRoute;
import uz.melisa.repository.ChatMemoryRepository;
import uz.melisa.service.client.CatalogServiceClient;
import uz.melisa.service.client.EmbeddingClient;
import uz.melisa.service.impl.ChatPostProcessService;
import uz.melisa.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uz.melisa.util.LangUtil.currentLang;
import static uz.melisa.util.StringUtil.trimToEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalMessageHandler {

    private final AiChatService aiChatService;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ChatPostProcessService chatPostProcessService;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingService embeddingService;
    private final CatalogServiceClient catalogServiceClient;

    public Map<Boolean, ProductBasedMessage> handleChatMessage(ChatUserMessageDTO chatUserMessage, Long userId) {
        Long chatId = chatUserMessage.getChatId();
        Long messageId = chatUserMessage.getMessageId();
        String input = trimToEmpty(chatUserMessage.getMessage());
        String conversationKey = "chat:" + chatId;
        String prevSummary = loadPrevSummary(chatId);
        boolean productBased = aiChatService.isProductBased(input);
        List<Long> productIds = new ArrayList<>();
        if (productBased) {
            OpenAiEmbeddingResponseDTO embedded = performEmbedding(input, messageId);
            List<ProductDTO> products = resolveProducts(userId, embedded);
            if (!products.isEmpty()) {
                productIds = products.stream().map(ProductDTO::getId).toList();
            }
            if (products.size() > 5) {
                products = products.subList(0, 5);
            }
            String modelInput = buildProductModelInput(prevSummary, input, products);
            AIResult result = aiChatService.chat(AiRoute.PRODUCT, conversationKey, modelInput);
            String answer = result == null ? "" : trimToEmpty(result.getText());
            chatPostProcessService.processClaude(
                    chatId, messageId, conversationKey, input, answer,
                    !products.isEmpty(), modelInput, null, null, result
            );
            return Map.of(!products.isEmpty(), new ProductBasedMessage(answer, productIds));
        }
        String modelInput = buildGeneralModelInput(prevSummary, input);
        AIResult result = aiChatService.chat(AiRoute.GENERAL, conversationKey, modelInput);
        String answer = result == null ? "" : trimToEmpty(result.getText());
        chatPostProcessService.processClaude(
                chatId, messageId, conversationKey, input, answer,
                false, modelInput, null, null, result
        );
        return Map.of(false, new ProductBasedMessage(answer, productIds));
    }

    /**
     * Runs the pre-model part of the chat flow (classification, embedding,
     * product lookup, prompt building) and returns everything the streaming
     * endpoint needs to start the model stream and persist results afterwards.
     */
    public ChatStreamPlan prepareChatStream(ChatUserMessageDTO chatUserMessage, Long userId) {
        Long chatId = chatUserMessage.getChatId();
        Long messageId = chatUserMessage.getMessageId();
        String input = trimToEmpty(chatUserMessage.getMessage());
        String conversationKey = "chat:" + chatId;
        String prevSummary = loadPrevSummary(chatId);

        if (!aiChatService.isProductBased(input)) {
            return new ChatStreamPlan(
                    AiRoute.GENERAL, conversationKey,
                    buildGeneralModelInput(prevSummary, input),
                    List.of(), false
            );
        }

        OpenAiEmbeddingResponseDTO embedded = performEmbedding(input, messageId);
        List<ProductDTO> products = resolveProducts(userId, embedded);
        if (products.size() > 5) {
            products = products.subList(0, 5);
        }
        List<Long> productIds = products.stream().map(ProductDTO::getId).toList();
        String modelInput = buildProductModelInput(prevSummary, input, products);
        return new ChatStreamPlan(AiRoute.PRODUCT, conversationKey, modelInput, productIds, !products.isEmpty());
    }

    private OpenAiEmbeddingResponseDTO performEmbedding(String input, Long messageId) {
        OpenAiEmbeddingResponseDTO embedded = embeddingClient.embed(input);
        embeddingService.saveEmbedding(messageId, embedded, input);
        return embedded;
    }

    private List<ProductDTO> resolveProducts(Long userId, OpenAiEmbeddingResponseDTO embedded) {
        if (embedded == null || embedded.getData() == null || embedded.getData().isEmpty()) {
            return List.of();
        }
        List<Float> embedding = embedded.getData().getFirst().getEmbedding();
        CommonResponse<List<ProductDTO>> response = catalogServiceClient.embeddingToProduct(
                userId,
                new EmbeddingProductSearchRequestDTO(
                        toFloatArray(embedding),
                        50,
                        currentLang()
                )
        );
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    private static float[] toFloatArray(List<Float> list) {
        if (list == null || list.isEmpty()) return new float[0];
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Float value = list.get(i);
            arr[i] = value == null ? 0f : value;
        }
        return arr;
    }

    private String loadPrevSummary(Long chatId) {
        if (chatId == null) return "";
        return chatMemoryRepository.findByChatId(chatId)
                .map(ChatMemory::getSummary)
                .map(StringUtil::trimToEmpty)
                .orElse("");
    }

    private String buildGeneralModelInput(String prevSummary, String userText) {
        String summary = trimToEmpty(prevSummary);
        String user = trimToEmpty(userText);

        if (summary.isEmpty()) return user;
        return "Summary:\n" + summary + "\n\nUser:\n" + user;
    }

    private String buildProductModelInput(String prevSummary, String userText, List<ProductDTO> products) {
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