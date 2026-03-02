package uz.melisa.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.product.ProductSuggestionDTO;
import uz.melisa.service.MessageProductSuggestionService;
import uz.melisa.util.ResponseUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/product-suggestion")
public class ProductSuggestionController {

    private final MessageProductSuggestionService messageProductSuggestionService;

    @Hidden
    @GetMapping("/{id}/products")
    public ResponseEntity<CommonResponse<List<ProductSuggestionDTO>>> getProductSuggestions(
            @PathVariable Long id
    ) {
        log.info("REST request to get suggestions id: {}", id);
        return ResponseUtil.buildResponseDTO(messageProductSuggestionService.getProductsByMessage(id));
    }
}
