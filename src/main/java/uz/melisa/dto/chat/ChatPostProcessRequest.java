package uz.melisa.dto.chat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPostProcessRequest {

    private Long chatId;
    private String conversationKey;

    private String prevSummary;
    private String userText;
    private String assistantText;

    private Double difficulty;
    private String requestLang;
    private String requestScript;

    private String provider;
    private String model;
    private String responseId;
    private Long createdEpoch;
    private String finishReason;

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    private Integer inputTokens;
    private Integer outputTokens;

    private Integer cacheCreationInputTokens;
    private Integer cacheReadInputTokens;

    private String rateLimitRaw;
    private String usageRaw;
    private String responseRaw;
}
