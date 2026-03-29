package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.embedding.VoyageEmbeddingRequestDTO;
import uz.melisa.dto.client.embedding.VoyageEmbeddingResponseDTO;
import uz.melisa.service.RestSenderService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingClient {

    private final ModelsProperties modelsProperties;
    private final RestSenderService restSenderService;

    public VoyageEmbeddingResponseDTO embed(String input) {
        VoyageEmbeddingRequestDTO request = buildRequest(input);
        log.info("The embedding request : {}", request);
        ModelsProperties.VoyageEmbedding m = modelsProperties.getVoyageEmbedding();
        String url = m.getBaseUrl();
        String authHeader = "Bearer " + m.getSecretKey();
        Map<String, String> headers = Map.of("Authorization", authHeader);

        return restSenderService.sendAndReceive(
                url,
                HttpMethod.POST,
                request,
                VoyageEmbeddingResponseDTO.class,
                headers
        );
    }

    private VoyageEmbeddingRequestDTO buildRequest(String input) {
        VoyageEmbeddingRequestDTO request = new VoyageEmbeddingRequestDTO();
        request.setInput(List.of(input));
        request.setModel(modelsProperties.getVoyageEmbedding().getModel());
        request.setInputType(modelsProperties.getVoyageEmbedding().getInputQueryType());
        return request;
    }
}
