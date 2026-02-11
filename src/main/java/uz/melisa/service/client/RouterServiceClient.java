package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.router.*;
import uz.melisa.service.RestSenderService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouterServiceClient {

    private final ModelsProperties modelsProperties;
    private final RestSenderService restSenderService;

    public RateDifficultyResponseDTO rateDifficulty(String message) {
        ModelsProperties.RouterModel m = modelsProperties.getRouterModel();
        String url = m.getBaseUrl() + m.getDifficultyPath();
        Map<String, String> headers = Map.of("x-api-key", m.getAuthKey());

        return restSenderService.sendAndReceive(
                url,
                HttpMethod.POST,
                new RateDifficultyRequestDTO(message),
                RateDifficultyResponseDTO.class,
                headers
        );
    }

    public SummarizeResponseDTO summarize(List<SummarizeMessageDTO> messages, String previousSummary) {
        ModelsProperties.RouterModel routerModel = modelsProperties.getRouterModel();
        String url = routerModel.getBaseUrl() + routerModel.getSummarizePath();
        Map<String, String> headers = Map.of("x-api-key", routerModel.getAuthKey());

        return restSenderService.sendAndReceive(
                url, HttpMethod.POST,
                new SummarizeRequestDTO(routerModel.getMaxTokens(), previousSummary, messages),
                SummarizeResponseDTO.class, headers
        );
    }
}
