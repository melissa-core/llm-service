package uz.melisa.dto.client.llama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlamaResponseChoicesDTO {

    private long index;
    @JsonProperty("finish_reason")
    private String finishReason;
    private LlamaResponseChoiceMessageDTO message;
}
