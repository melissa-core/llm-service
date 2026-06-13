package uz.melisa.dto.client.fitrat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FitratDetectLangResponseDTO {

    private String lang;
    @JsonProperty("request_text")
    private String requestText;
}
