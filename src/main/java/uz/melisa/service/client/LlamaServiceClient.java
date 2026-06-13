package uz.melisa.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import uz.melisa.config.ModelsProperties;
import uz.melisa.dto.client.llama.LlamaChatRequestDTO;
import uz.melisa.dto.client.llama.LlamaChatResponseDTO;
import uz.melisa.dto.client.llama.LlamaMessagesDTO;
import uz.melisa.service.RestSenderService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlamaServiceClient {

    private final ModelsProperties modelsProperties;
    private final RestSenderService restSenderService;

    public static final String USER_MESSAGE_ROLE = "user";

    public LlamaChatResponseDTO chat(String message) {
        ModelsProperties.LlamaModel m = modelsProperties.getLlamaModel();
        String url = m.getBaseUrl() + m.getChatPath();
        Map<String, String> headers = Map.of("Authorization", "Bearer " + m.getAuthKey());

        LlamaChatRequestDTO req = new LlamaChatRequestDTO(
                m.getModel(),
                m.getMaxTokens(),
                m.getTemperature(),
                List.of(new LlamaMessagesDTO(USER_MESSAGE_ROLE, message))
        );

        return restSenderService.sendAndReceive(
                url,
                HttpMethod.POST,
                req,
                LlamaChatResponseDTO.class,
                headers
        );
    }
}
