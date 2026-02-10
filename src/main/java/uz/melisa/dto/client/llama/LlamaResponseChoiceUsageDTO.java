package uz.melisa.dto.client.llama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlamaResponseChoiceUsageDTO {

    @JsonProperty("prompt_tokens")
    private int promptTokens;
    @JsonProperty("total_tokens")
    private int totalTokens;
    @JsonProperty("completion_tokens")
    private int completionTokens;
}
