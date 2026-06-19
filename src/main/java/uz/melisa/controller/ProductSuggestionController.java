package uz.melisa.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Hidden
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/product-suggestion")
@Tag(name = "Chat - Product Suggestions", description = "Internal API. Returns the product suggestions (product id + display position) previously attached to a chat message. Service-to-service only and hidden from Swagger UI; the request is authenticated (resolved from the security context).")
public class ProductSuggestionController {

    private final MessageProductSuggestionService messageProductSuggestionService;

    @Hidden
    @Operation(
            summary = "Get message product suggestions",
            description = "Internal API. Returns the ordered list of product suggestions (product id and position) stored for the given message id. Returns an empty list when the message has no suggestions. Hidden from Swagger UI; requires authentication. No specific permission code or client type is enforced beyond an authenticated request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product suggestions for the message (empty list when none exist)"),
            @ApiResponse(responseCode = "400", description = "Malformed message id path variable (type mismatch)"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/{id}/products")
    public ResponseEntity<CommonResponse<List<ProductSuggestionDTO>>> getProductSuggestions(
            @Parameter(description = "Message id whose stored product suggestions are returned", required = true, example = "1024")
            @PathVariable Long id
    ) {
        log.info("REST request to get suggestions id: {}", id);
        return ResponseUtil.buildResponseDTO(messageProductSuggestionService.getProductsByMessage(id));
    }
}
