package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.fitrat.FitratDetectLangRequestDTO;
import uz.melisa.dto.client.fitrat.FitratDetectLangResponseDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateRequestDTO;
import uz.melisa.dto.client.fitrat.FitratTransliterateResponseDTO;
import uz.melisa.service.RestSenderService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FitratServiceClient {

    private final ModelsProperties modelsProperties;
    private final RestSenderService restSenderService;

    public FitratDetectLangResponseDTO detect(String message) {
        ModelsProperties.FitratModel m = modelsProperties.getFitratModel();
        String url = m.getBaseUrl() + m.getDetectPath();
        Map<String, String> headers = Map.of("x-api-key", m.getAuthKey());

        return restSenderService.sendAndReceive(
                url,
                HttpMethod.POST,
                new FitratDetectLangRequestDTO(message),
                FitratDetectLangResponseDTO.class,
                headers
        );
    }

    public FitratTransliterateResponseDTO transliterate(FitratTransliterateRequestDTO request) {
        ModelsProperties.FitratModel m = modelsProperties.getFitratModel();
        String url = m.getBaseUrl() + m.getTransliteratePath();
        Map<String, String> headers = Map.of("x-api-key", m.getAuthKey());

        return restSenderService.sendAndReceive(
                url,
                HttpMethod.POST,
                request,
                FitratTransliterateResponseDTO.class,
                headers
        );
    }
}
