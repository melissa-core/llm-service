package uz.melisa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.melisa.dto.claude.ClaudeChatRequest;
import uz.melisa.dto.claude.ClaudeChatResponse;
import uz.melisa.service.ClaudeChatService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/claude")
public class ClaudeChatController {

    private final ClaudeChatService service;

    @PostMapping(value = "/free-chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ClaudeChatResponse chat(@Valid @RequestBody ClaudeChatRequest request) {
        String answer = service.chatUzbek(request.getMessage());
        return new ClaudeChatResponse(answer);
    }
}
