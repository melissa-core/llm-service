package uz.melisa.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.chat.ChatDTO;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.chat.CreateChatRequestDTO;
import uz.melisa.dto.common.CommonResponse;

public interface ChatService {

    CommonResponse<ResponseMessageDTO> createChat(CreateChatRequestDTO createChatRequestDTO);

    Page<ChatDTO> getChatPages(Pageable pageable);

    Page<ChatMessagesDTO> getChatMessages(Long id, Pageable pageable);

    CommonResponse<ChatDTO> getChatById(Long id);
}
