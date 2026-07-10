package uz.melisa.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.melisa.dto.client.catalog.EmbeddingProductSearchRequestDTO;
import uz.melisa.dto.client.catalog.ProductDTO;
import uz.melisa.dto.client.embedding.OpenAiEmbeddingDataDTO;
import uz.melisa.dto.client.embedding.OpenAiEmbeddingResponseDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.embedding.EmbeddingDetailsDTO;
import uz.melisa.service.client.CatalogServiceClient;
import uz.melisa.service.client.EmbeddingClient;

import java.util.List;

import static uz.melisa.util.SecurityUtil.getCurrentUserId;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/embedding")
@Hidden
public class EmbeddingController {

    private final EmbeddingClient embeddingClient;
    private final CatalogServiceClient catalogServiceClient;

    @PostMapping("/product-details")
    public void getEmbedding(
            @RequestBody EmbeddingDetailsDTO embeddingDetailsDTO
    ) {
        Long userId = getCurrentUserId();
        List<String> texts = embeddingDetailsDTO.getTexts();
        String lang = embeddingDetailsDTO.getLang();
        log.info("REST request to get embedding for text: {}, {}", texts, lang);
        OpenAiEmbeddingResponseDTO embed = embeddingClient.embed(texts);
        log.info("Embedding response: {}", embed);
        OpenAiEmbeddingDataDTO embedded = embed.getData().getFirst();
        EmbeddingProductSearchRequestDTO embeddingProductSearchRequestDTO = new EmbeddingProductSearchRequestDTO();
        embeddingProductSearchRequestDTO.setEmbeddings(embedded.getEmbedding());
        embeddingProductSearchRequestDTO.setLimit(50);
        embeddingProductSearchRequestDTO.setLang(lang);
        log.info("Sending request to get product for embedding: {}", embeddingProductSearchRequestDTO);
        CommonResponse<List<ProductDTO>> listCommonResponse = catalogServiceClient.embeddingToProduct(userId, embeddingProductSearchRequestDTO);
        log.info("Product response: {}", listCommonResponse);
    }
}
