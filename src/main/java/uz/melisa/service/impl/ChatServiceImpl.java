package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.melisa.domain.Chat;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.chat.ChatDTO;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.chat.CreateChatRequestDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.ChatService;

import static uz.melisa.specifications.ChatSpecifications.byUser;
import static uz.melisa.util.SecurityUtil.getCurrentUserId;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Override
    public CommonResponse<ResponseMessageDTO> createChat(CreateChatRequestDTO createChatRequestDTO) {
        Long currentUserId = getCurrentUserId();
        Chat chat = new Chat();
        chat.setUserId(currentUserId);
        chat.setTitle(createChatRequestDTO.getTitle());
        chatRepository.save(chat);
        return CommonResponse.success(new ResponseMessageDTO("Chat saved successfully"));
    }

    @Override
    public Page<ChatDTO> getChatPages(Pageable pageable) {
        Long userId = getCurrentUserId();
        return chatRepository.findAll(byUser(userId), pageable)
                .map(chat -> new ChatDTO(chat.getId(), chat.getTitle()));
    }

    @Override
    public Page<ChatMessagesDTO> getChatMessages(Long id, Pageable pageable) {
        Long currentUserId = getCurrentUserId();

        chatRepository.findByIdAndUserIdAndIsDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new ItemNotFoundException("Chat not found"));

        return messageRepository.findChatMessages(id, currentUserId, pageable);
    }

    @Override
    public CommonResponse<ChatDTO> getChatById(Long id) {
        Long currentUserId = getCurrentUserId();

        Chat chat = chatRepository.findByIdAndUserIdAndIsDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new ItemNotFoundException("Chat not found"));

        return CommonResponse.success(new ChatDTO(chat.getId(), chat.getTitle()));
    }
}
