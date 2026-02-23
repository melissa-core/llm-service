package uz.melisa.dto.client.router;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodIntentResponseDTO {

    @JsonProperty("is_food")
    private Boolean isFood;
}
