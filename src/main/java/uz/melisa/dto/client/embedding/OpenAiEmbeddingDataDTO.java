package uz.melisa.dto.client.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiEmbeddingDataDTO {

    private String object;
    @ToString.Exclude
    private float[] embedding;
    private Integer index;
}