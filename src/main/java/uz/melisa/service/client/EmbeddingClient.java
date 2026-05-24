package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.embedding.OpenAiEmbeddingRequestDTO;
import uz.melisa.dto.client.embedding.OpenAiEmbeddingResponseDTO;
import uz.melisa.service.RestSenderService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingClient {

    private final ModelsProperties modelsProperties;
    private final RestSenderService restSenderService;

    public OpenAiEmbeddingResponseDTO embed(String input) {
        return embed(List.of(input));
    }

    public OpenAiEmbeddingResponseDTO embed(List<String> inputs) {
        OpenAiEmbeddingRequestDTO request = buildRequest(inputs);

        ModelsProperties.Embedding m = modelsProperties.getEmbedding();

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + m.getSecretKey(),
                "Content-Type", "application/json"
        );

        log.info("Sending embedding request. model={}, inputCount={}, dimensions={}",
                m.getModel(), inputs == null ? 0 : inputs.size(), m.getDimensions());

        return restSenderService.sendAndReceive(
                m.getBaseUrl(),
                HttpMethod.POST,
                request,
                OpenAiEmbeddingResponseDTO.class,
                headers
        );
    }

    private OpenAiEmbeddingRequestDTO buildRequest(List<String> inputs) {
        ModelsProperties.Embedding m = modelsProperties.getEmbedding();

        OpenAiEmbeddingRequestDTO request = new OpenAiEmbeddingRequestDTO();
        request.setModel(m.getModel());
        request.setInput(inputs);
        request.setDimensions(m.getDimensions());
        request.setEncodingFormat("float");

        return request;
    }
}