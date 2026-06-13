package uz.melisa.dto.client.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiEmbeddingRequestDTO {

    private String model;

    private List<String> input;

    private Integer dimensions;

    @JsonProperty("encoding_format")
    private String encodingFormat;
}