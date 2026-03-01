package uz.melisa.dto.client.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmbeddingProductSearchRequestDTO {

    @NotNull
    @ToString.Exclude
    private float[] embeddings;
    private int limit;
    @NotBlank
    private String lang;
}
