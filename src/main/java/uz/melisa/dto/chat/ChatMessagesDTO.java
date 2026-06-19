package uz.melisa.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.melisa.enums.MessageContentType;

import java.sql.Timestamp;

@Schema(description = "A single message entry within a chat conversation history")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessagesDTO {

    @Schema(description = "Unique identifier of the message", example = "1024")
    private Long messageId;

    @Schema(description = "Textual content of the message", example = "Show me red leather handbags")
    private String messageText;

    @Schema(description = "Type of the message", example = "TEXT")
    private String messageType;

    @Schema(description = "Authority that produced the message (e.g. user or model)", example = "USER")
    private String messageAuthorityType;

    @Schema(description = "Content category of the message", example = "PRODUCT")
    private MessageContentType contentType;

    @Schema(description = "Timestamp when the message was created", example = "2026-06-19T14:30:00.000+05:00")
    private Timestamp createdAt;
}
