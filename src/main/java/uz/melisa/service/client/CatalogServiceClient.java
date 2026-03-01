package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ApplicationProperties;
import uz.melisa.dto.client.catalog.EmbeddingProductSearchRequestDTO;
import uz.melisa.dto.client.catalog.ProductDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.service.GlobalProxyService;
import uz.melisa.service.RestSenderService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogServiceClient {

    private final ApplicationProperties applicationProperties;
    private final GlobalProxyService globalProxyService;
    private final RestSenderService restSenderService;

    public CommonResponse<List<ProductDTO>> embeddingToProduct(Long userId, EmbeddingProductSearchRequestDTO request) {
        ApplicationProperties.Catalog catalog = applicationProperties.getCatalog();
        Map<String, String> headers = globalProxyService.buildInternalHeaders(userId);
        String url = catalog.getCatalogServiceUrl() + catalog.getCatalogEmbeddingPath();
        return restSenderService.sendAndReceive(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<CommonResponse<List<ProductDTO>>>() {
                },
                headers
        );
    }
}
