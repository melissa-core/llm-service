package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.router.RateDifficultyRequestDTO;
import uz.melisa.dto.client.router.RateDifficultyResponseDTO;
import uz.melisa.service.RestSenderService;

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
}
