package uz.melisa.dto.client.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageEmbeddingData {

    private String object;
    @ToString.Exclude
    private List<Float> embedding;
    private Integer index;
}
