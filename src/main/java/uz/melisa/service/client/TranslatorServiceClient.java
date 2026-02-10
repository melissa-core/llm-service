package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.translator.TranslatorTextRequestDTO;
import uz.melisa.dto.client.translator.TranslatorTextResponseDTO;
import uz.melisa.service.RestSenderService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslatorServiceClient {

    private final ModelsProperties modelsProperties;
    private final RestSenderService restSenderService;

    public TranslatorTextResponseDTO translate(TranslatorTextRequestDTO request) {
        ModelsProperties.TranslatorModel m = modelsProperties.getTranslatorModel();
        String url = m.getBaseUrl() + m.getTranslatorPath();
        Map<String, String> headers = Map.of("X-API-Key", m.getAuthKey());

        return restSenderService.sendAndReceive(
                url,
                HttpMethod.POST,
                request,
                TranslatorTextResponseDTO.class,
                headers
        );
    }
}
