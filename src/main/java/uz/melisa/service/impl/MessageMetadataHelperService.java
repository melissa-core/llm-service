package uz.melisa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.domain.MessageMetadata;
import uz.melisa.dto.claude.ClaudeResult;
import uz.melisa.dto.client.llama.LlamaChatResponseDTO;
import uz.melisa.repository.MessageMetadataRepository;

import static uz.melisa.util.StringUtil.trimToEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageMetadataHelperService {

    private final MessageMetadataRepository messageMetadataRepository;
    private final ObjectMapper objectMapper;

    public void saveClaude(Long chatId,
                           Long messageId,
                           String conversationKey,
                           ClaudeResult result,
                           String requestRaw,
                           boolean isFoodContent,
                           String requestLang,
                           String requestScript) {

        if (result == null) return;

        saveMessageMetadata(
                chatId,
                messageId,
                conversationKey,
                "claude",
                result.getModel(),
                result.getResponseId(),
                null,
                result.getFinishReason(),
                requestRaw,
                isFoodContent,
                requestLang,
                requestScript,
                result.getPromptTokens(),
                result.getCompletionTokens(),
                result.getTotalTokens(),
                result.getInputTokens(),
                result.getOutputTokens(),
                result.getCacheCreationInputTokens(),
                result.getCacheReadInputTokens(),
                result.getRateLimitRaw(),
                result.getUsageRaw(),
                result.getResponseRaw()
        );
    }

    public void saveLlama(Long chatId,
                          Long messageId,
                          String conversationKey,
                          LlamaChatResponseDTO llamaResp,
                          boolean isFoodContent,
                          String requestLang,
                          String requestScript) {

        saveMessageMetadata(
                chatId,
                messageId,
                conversationKey,
                "llama",
                trimToEmpty(llamaResp == null ? null : llamaResp.getModel()),
                llamaResp == null ? null : llamaResp.getId(),
                llamaResp == null ? null : llamaResp.getCreated(),
                extractFinishReason(llamaResp),
                null,
                isFoodContent,
                requestLang,
                requestScript,
                promptTokens(llamaResp),
                completionTokens(llamaResp),
                totalTokens(llamaResp),
                promptTokens(llamaResp),
                completionTokens(llamaResp),
                null,
                null,
                null,
                toJson(llamaResp == null ? null : llamaResp.getUsage()),
                toJson(llamaResp)
        );
    }

    public void saveMessageMetadata(
            Long chatId,
            Long messageId,
            String conversationKey,
            String provider,
            String model,
            String responseId,
            Long createdEpoch,
            String finishReason,
            String requestRaw,
            boolean isFoodContent,
            String requestLang,
            String requestScript,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Integer inputTokens,
            Integer outputTokens,
            Integer cacheCreationInputTokens,
            Integer cacheReadInputTokens,
            String rateLimitRaw,
            String usageRaw,
            String responseRaw
    ) {
        if (chatId == null) return;
        if (provider == null || provider.isBlank()) return;
        if (model == null || model.isBlank()) return;

        MessageMetadata meta = MessageMetadata.builder()
                .chatId(chatId)
                .messageId(messageId)
                .conversationKey(trimToEmpty(conversationKey))
                .provider(trimToEmpty(provider))
                .model(trimToEmpty(model))
                .responseId(responseId)
                .createdEpoch(createdEpoch)
                .finishReason(trimToEmpty(finishReason))
                .requestRaw(trimToEmpty(requestRaw))
                .isFoodContent(isFoodContent)
                .detectedLang(trimToEmpty(requestLang))
                .detectedScript(trimToEmpty(requestScript))
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .cacheCreationInputTokens(cacheCreationInputTokens)
                .cacheReadInputTokens(cacheReadInputTokens)
                .rateLimitRaw(rateLimitRaw)
                .usageRaw(usageRaw)
                .responseRaw(responseRaw)
                .build();

        messageMetadataRepository.save(meta);
    }

    private String extractFinishReason(LlamaChatResponseDTO resp) {
        if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()) return null;
        var c = resp.getChoices().getFirst();
        return c == null ? null : trimToEmpty(c.getFinishReason());
    }

    private Integer promptTokens(LlamaChatResponseDTO resp) {
        if (resp == null || resp.getUsage() == null) return null;
        return resp.getUsage().getPromptTokens();
    }

    private Integer completionTokens(LlamaChatResponseDTO resp) {
        if (resp == null || resp.getUsage() == null) return null;
        return resp.getUsage().getCompletionTokens();
    }

    private Integer totalTokens(LlamaChatResponseDTO resp) {
        if (resp == null || resp.getUsage() == null) return null;
        return resp.getUsage().getTotalTokens();
    }

    private String toJson(Object obj) {
        try {
            if (obj == null) return null;
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
