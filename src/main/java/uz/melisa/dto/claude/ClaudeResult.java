package uz.melisa.dto.claude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeResult {

    private String text;

    private String responseId;
    private String model;
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
