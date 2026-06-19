package uz.melisa.dto.claude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for sending a chat message as a guest (unauthenticated) user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuestChatRequest {
    @Schema(description = "Message text sent by the guest user", example = "What handbags do you have in stock?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String message;
}
