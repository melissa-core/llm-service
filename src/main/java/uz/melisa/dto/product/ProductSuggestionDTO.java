package uz.melisa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "A single product suggestion entry pairing a product with its ranking position in the suggestion list")
public class ProductSuggestionDTO {

    @Schema(description = "Identifier of the suggested product", example = "1024")
    private Long productId;

    @Schema(description = "Zero-based ranking position of this product within the suggestion list", example = "0")
    private int position;
}
