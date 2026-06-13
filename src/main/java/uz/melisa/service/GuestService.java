package uz.melisa.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.claude.GuestChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;

public interface GuestService {

    CommonResponse<GuestMessageResponseDTO> guestSendMessage(String deviceId, GuestChatRequest request);

    CommonResponse<Page<ChatMessagesDTO>> getMessages(String deviceId, Pageable pageable);
}
