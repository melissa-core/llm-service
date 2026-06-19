package uz.melisa.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.melisa.enums.MessageContentType;

@Schema(description = "Response payload returned after sending a chat message")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponseDTO {

    @Schema(description = "Assistant reply text generated for the message", example = "Here are three wallets that pair well with that bag.")
    private String message;

    @Schema(description = "Identifier of the chat the message belongs to", example = "57")
    private Long chatId;

    @Schema(description = "Identifier of the stored user message", example = "1024")
    private Long userMessageId;

    @Schema(description = "Identifier of the stored model (assistant) message", example = "1025")
    private Long modelMessageId;

    @Schema(description = "Content category of the assistant reply", example = "PRODUCT")
    private MessageContentType contentType;
}
