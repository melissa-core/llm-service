package uz.melisa.dto.guest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Response payload returned for a guest chat message")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuestMessageResponseDTO {

    @Schema(description = "Assistant reply text for the guest user", example = "We have several leather handbags available right now.")
    private String message;
}
