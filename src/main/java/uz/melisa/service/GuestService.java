package uz.melisa.service;

import uz.melisa.dto.claude.ClaudeChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;

public interface GuestService {

    CommonResponse<GuestMessageResponseDTO> chatWithClaude(ClaudeChatRequest request);
}
