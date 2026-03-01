package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "message_metadata")
@Builder
public class MessageMetadata extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private Message message;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "conversation_key", length = 128)
    private String conversationKey;

    @Column(name = "provider", length = 16, nullable = false)
    private String provider;

    @Column(name = "model", length = 128, nullable = false)
    private String model;

    @Column(name = "response_id", length = 128)
    private String responseId;

    @Column(name = "created_epoch")
    private Long createdEpoch;

    @Column(name = "finish_reason", length = 64)
    private String finishReason;

    @Column(name = "is_food_content")
    private boolean isFoodContent;

    @Column(name = "request_raw", columnDefinition = "TEXT")
    private String requestRaw;

    @Column(name = "detected_lang", length = 32)
    private String detectedLang;

    @Column(name = "detected_script", length = 16)
    private String detectedScript;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cache_creation_input_tokens")
    private Integer cacheCreationInputTokens;

    @Column(name = "cache_read_input_tokens")
    private Integer cacheReadInputTokens;

    @Column(name = "rate_limit_raw", columnDefinition = "TEXT")
    private String rateLimitRaw;

    @Column(name = "usage_raw", columnDefinition = "TEXT")
    private String usageRaw;

    @Column(name = "response_raw", columnDefinition = "TEXT")
    private String responseRaw;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
