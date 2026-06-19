package uz.melisa.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body for creating a new chat conversation")
public class CreateChatRequestDTO {

    @Schema(description = "Title of the chat conversation", example = "Order help", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String title;
}
