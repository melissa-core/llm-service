package uz.melisa.service;

import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.product.ProductSuggestionDTO;

import java.util.List;

public interface MessageProductSuggestionService {

    void saveProductSuggestion(long messageId, List<Long> productIds);

    CommonResponse<List<ProductSuggestionDTO>> getProductsByMessage(Long id);
}
