package uz.melisa.dto.client.llama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlamaChatRequestDTO {

    private String model;
    @JsonProperty("max_tokens")
    private int maxTokens;
    private double temperature;
    private List<LlamaMessagesDTO> messages;
}
