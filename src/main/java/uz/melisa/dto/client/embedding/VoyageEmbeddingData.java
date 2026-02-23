package uz.melisa.dto.client.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageEmbeddingData {

    private String object;
    private List<Float> embedding;
    private Integer index;
}
