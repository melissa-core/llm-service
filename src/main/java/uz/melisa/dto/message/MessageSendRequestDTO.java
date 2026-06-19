package uz.melisa.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for sending a chat message from an authenticated user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageSendRequestDTO {

    @Schema(description = "Message text to send, up to 1024 characters", example = "Recommend a wallet to match this bag", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 1024)
    private String message;

    @Schema(description = "Identifier of the chat to post the message to; omit to start a new chat", example = "57")
    private Long chatId;
}
