package uz.melisa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.ChatMemory;
import uz.melisa.domain.MessageLanguageDetails;
import uz.melisa.dto.claude.AIResult;
import uz.melisa.dto.client.fitrat.FitratDetectLangResponseDTO;
import uz.melisa.dto.client.llama.LlamaChatResponseDTO;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.repository.ChatMemoryRepository;
import uz.melisa.repository.MessageLanguageDetailsRepository;
import uz.melisa.service.AiChatService;
import uz.melisa.service.MessageProductSuggestionService;
import uz.melisa.util.StringUtil;

import java.util.List;

import static uz.melisa.constants.PrivacyConstants.SYSTEM_COMMAND;
import static uz.melisa.util.StringUtil.trimToEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPostProcessService {

    private final MessageMetadataHelperService messageMetadataHelperService;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ObjectMapper objectMapper;
    private final MessageLanguageDetailsRepository messageLanguageDetailsRepository;
    private final AiChatService aiChatService;
    private final MessageProductSuggestionService messageProductSuggestionService;

    @Async("postProcessExecutor")
    @Transactional
    public void processClaude(Long chatId,
                              Long messageId,
                              String conversationKey,
                              String userText,
                              String assistantText,
                              boolean isFoodContent,
                              String modelInput,
                              String requestLang,
                              String requestScript,
                              AIResult result) {
        if (chatId == null) return;

        try {
            String requestRaw = buildClaudeRequestRaw(conversationKey, SYSTEM_COMMAND, modelInput);
            messageMetadataHelperService.saveClaude(
                    chatId, messageId, conversationKey, result, requestRaw, isFoodContent, requestLang, requestScript
            );
        } catch (Exception e) {
            log.warn("metadata save failed chatId={} provider=claude", chatId, e);
        }

        try {
            updateSummary(chatId, userText, assistantText);
        } catch (Exception e) {
            log.warn("summary save failed chatId={}", chatId, e);
        }
    }

    @Async("postProcessExecutor")
    @Transactional
    public void processRecommendedMessage(Long messageId, List<Long> productIds) {
        if (messageId == null || productIds == null || productIds.isEmpty()) return;
        try {
            messageProductSuggestionService.saveProductSuggestion(messageId, productIds);
        } catch (Exception e) {
            log.warn("message product suggestion save failed messageId={} productIds={}", messageId, productIds, e);
        }
    }

    @Async("postProcessExecutor")
    @Transactional
    public void processLlama(Long chatId,
                             Long messageId,
                             String conversationKey,
                             String userText,
                             String assistantText,
                             boolean isFoodContent,
                             String requestLang,
                             String requestScript,
                             LlamaChatResponseDTO llamaResp) {
        if (chatId == null) return;

        try {
            messageMetadataHelperService.saveLlama(
                    chatId, messageId, conversationKey, llamaResp, isFoodContent, requestLang, requestScript
            );
        } catch (Exception e) {
            log.warn("metadata save failed chatId={} provider=llama", chatId, e);
        }

        try {
            updateSummary(chatId, userText, assistantText);
        } catch (Exception e) {
            log.warn("summary save failed chatId={}", chatId, e);
        }
    }

    @Async("postProcessExecutor")
    @Transactional
    public void processLangDetection(FitratDetectLangResponseDTO langDetectResp, Long chatId,
                                     Long messageId, String detectedLang, String detectedScript,
                                     MessageAuthorityType messageAuthorityType, String text) {
        if (langDetectResp == null) return;

        MessageLanguageDetails languageDetails = new MessageLanguageDetails();
        languageDetails.setChatId(chatId);
        languageDetails.setMessageId(messageId);
        languageDetails.setDetectedLanguage(detectedLang);
        languageDetails.setDetectedScript(detectedScript);
        languageDetails.setMessageAuthorityType(messageAuthorityType);
        languageDetails.setText(trimToEmpty(text));
        try {
            messageLanguageDetailsRepository.save(languageDetails);
        } catch (Exception e) {
            log.warn("metadata save failed chatId={} provider=languageDetails", chatId, e);
        }
    }

    private String buildClaudeRequestRaw(String conversationKey, String system, String safeInput) {
        try {
            var node = objectMapper.createObjectNode();
            node.put("conversationKey", conversationKey);
            node.put("system", system);
            node.put("user", safeInput);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private void updateSummary(Long chatId, String userText, String assistantText) {
        String prevSummary = chatMemoryRepository.findByChatId(chatId)
                .map(ChatMemory::getSummary)
                .map(StringUtil::trimToEmpty)
                .orElse("");

        String summary = trimToEmpty(
                aiChatService.summarize(prevSummary, userText, assistantText)
        );

        if (summary.isEmpty()) {
            return;
        }

        ChatMemory mem = chatMemoryRepository.findByChatId(chatId)
                .orElseGet(() -> ChatMemory.builder().chatId(chatId).build());

        mem.setSummary(summary);
        chatMemoryRepository.save(mem);
    }
}