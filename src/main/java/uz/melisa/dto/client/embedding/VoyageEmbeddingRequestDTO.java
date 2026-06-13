package uz.melisa.dto.client.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageEmbeddingRequestDTO {

    private String model;
    private List<String> input;
    @JsonProperty("input_type")
    private String inputType;
}
