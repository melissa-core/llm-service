package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.dto.claude.ClaudeChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;
import uz.melisa.service.ClaudeChatService;
import uz.melisa.service.GuestService;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {

    private final ClaudeChatService claudeChatService;

    @Override
    public CommonResponse<GuestMessageResponseDTO> chatWithClaude(ClaudeChatRequest request) {
        String claudeResponse = claudeChatService.chatWithClaude(request.getMessage());
        return CommonResponse.success(new GuestMessageResponseDTO(claudeResponse));
    }
}
