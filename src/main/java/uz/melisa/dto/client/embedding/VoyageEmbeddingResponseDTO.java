package uz.melisa.dto.client.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageEmbeddingResponseDTO {

    private String object;
    private List<VoyageEmbeddingData> data;
    private String model;
    private VoyageUsage usage;
}
