package uz.melisa.dto.client.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiEmbeddingResponseDTO {

    private String object;
    private List<OpenAiEmbeddingDataDTO> data;
    private String model;
    private OpenAiEmbeddingUsageDTO usage;
}