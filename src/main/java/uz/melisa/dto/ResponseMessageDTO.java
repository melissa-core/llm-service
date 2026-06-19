package uz.melisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Generic message response returned by write operations")
public class ResponseMessageDTO {

    @Schema(description = "Human-readable result message", example = "Operation completed successfully")
    private String message;
}
