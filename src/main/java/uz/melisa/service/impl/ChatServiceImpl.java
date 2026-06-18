package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Chat;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.chat.ChatDTO;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.chat.ChatPageDTO;
import uz.melisa.dto.chat.CreateChatRequestDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.enums.MessageCode;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.ChatService;
import uz.melisa.service.LocalizationService;

import java.util.Optional;

import static uz.melisa.util.SecurityUtil.getCurrentUserId;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final LocalizationService localizationService;

    @Override
    public CommonResponse<ResponseMessageDTO> createChat(CreateChatRequestDTO createChatRequestDTO) {
        Long currentUserId = getCurrentUserId();
        Chat chat = new Chat();
        chat.setUserId(currentUserId);
        chat.setTitle(createChatRequestDTO.getTitle());
        chatRepository.save(chat);
        return CommonResponse.success(new ResponseMessageDTO(localizationService.getMessage(MessageCode.CHAT_SAVED)));
    }

    @Transactional(readOnly = true)
    @Override
    public CommonResponse<Page<ChatPageDTO>> getChatPages(Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<ChatPageDTO> chatPages = chatRepository.findChatPagesOrdered(userId, MessageAuthorityType.USER, pageable);
        return CommonResponse.success(chatPages);
    }

    @Transactional(readOnly = true)
    @Override
    public CommonResponse<Page<ChatMessagesDTO>> getChatMessages(Long id, Pageable pageable) {
        Long currentUserId = getCurrentUserId();

        chatRepository.findByIdAndUserIdAndIsDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new ItemNotFoundException(MessageCode.CHAT_NOT_FOUND));

        Page<ChatMessagesDTO> chatMessages = messageRepository.findChatMessages(id, currentUserId, pageable);
        return CommonResponse.success(chatMessages);
    }

    @Override
    public CommonResponse<ChatDTO> getChatById(Long id) {
        Long currentUserId = getCurrentUserId();

        Chat chat = chatRepository.findByIdAndUserIdAndIsDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new ItemNotFoundException(MessageCode.CHAT_NOT_FOUND));

        return CommonResponse.success(new ChatDTO(chat.getId(), chat.getTitle(), chat.getCreatedAt()));
    }

    @Transactional
    @Override
    public CommonResponse<ResponseMessageDTO> updateChat(Long id, CreateChatRequestDTO createChatRequestDTO) {
        if (id == null) throw new ItemNotFoundException(MessageCode.CHAT_NOT_FOUND);

        Chat chat = chatRepository.findByIdAndUserIdAndIsDeletedFalse(id, getCurrentUserId())
                .orElseThrow(() -> new ItemNotFoundException(MessageCode.CHAT_NOT_FOUND));

        Chat updateChat = Chat.builder()
                .id(chat.getId())
                .title(createChatRequestDTO.getTitle())
                .userId(chat.getUserId())
                .build();
        chatRepository.save(updateChat);

        return CommonResponse.success(new ResponseMessageDTO(localizationService.getMessage(MessageCode.CHAT_UPDATED)));
    }

    @Transactional
    @Override
    public CommonResponse<ResponseMessageDTO> deleteChat(Long id) {
        if (id == null) throw new ItemNotFoundException(MessageCode.CHAT_NOT_FOUND);

        Chat chat = chatRepository.findByIdAndUserIdAndIsDeletedFalse(id, getCurrentUserId())
                .orElseThrow(() -> new ItemNotFoundException(MessageCode.CHAT_NOT_FOUND));

        chat.setDeleted(true);
        chatRepository.save(chat);
        return CommonResponse.success(new ResponseMessageDTO(localizationService.getMessage(MessageCode.CHAT_DELETED)));
    }

    @Transactional
    @Override
    public CommonResponse<ResponseMessageDTO> activateChat(String key) {
        Long currentUserId = getCurrentUserId();
        Optional<Chat> chatOpt = chatRepository.findTop1ByDeviceIdAndIsDeletedFalse(key);
        if (chatOpt.isEmpty()) {
            log.warn("Temporary chat is not found");
            return CommonResponse.success(new ResponseMessageDTO(localizationService.getMessage(MessageCode.CHAT_TEMPORARY_NOT_FOUND)));
        }

        Chat chat = chatOpt.get();
        chatRepository.activateChatByDeviceId(currentUserId, chat.getId());
        messageRepository.setUserId(currentUserId, chat.getId());
        log.info("Chat activated successfully");
        return CommonResponse.success(new ResponseMessageDTO(localizationService.getMessage(MessageCode.CHAT_ACTIVATED)));
    }
}
