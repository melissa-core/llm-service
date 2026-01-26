package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Chat;
import uz.melisa.domain.Message;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.ClaudeChatService;
import uz.melisa.service.MessageService;

import static uz.melisa.util.ChatUtil.buildChat;
import static uz.melisa.util.ChatUtil.getChatTitle;
import static uz.melisa.util.MessageUtil.buildModelMessage;
import static uz.melisa.util.MessageUtil.buildUserMessage;
import static uz.melisa.util.SecurityUtil.getCurrentUserId;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ClaudeChatService claudeChatService;

    @Transactional
    @Override
    public CommonResponse<MessageResponseDTO> sendMessage(MessageSendRequestDTO messageSendRequestDTO) {
        Long userId = getCurrentUserId();
        Message message;
        Chat chat;
        if (messageSendRequestDTO.getChatId() != null) {
            chat = chatRepository.findByIdAndUserIdAndIsDeletedFalse(messageSendRequestDTO.getChatId(), userId)
                    .orElseThrow(() -> new ItemNotFoundException("Chat not found"));
        } else {
            chat = chatRepository.save(buildChat(userId, getChatTitle(messageSendRequestDTO.getMessage())));
            chatRepository.flush();
        }
        message = messageRepository.save(buildUserMessage(messageSendRequestDTO.getMessage(), chat.getId(), userId));
        messageRepository.flush();
        String modelResponseMessage = claudeChatService.chatWithClaude(message.getText());
        Message modelMessageResponse = messageRepository.save(buildModelMessage(modelResponseMessage, message.getChatId(), userId));
        messageRepository.flush();
        return CommonResponse.success(new MessageResponseDTO(modelMessageResponse.getText(), chat.getId(), message.getId(), modelMessageResponse.getId()));
    }

    @Transactional
    @Override
    public CommonResponse<ResponseMessageDTO> deleteMessage(long id) {
        Long userId = getCurrentUserId();
        Message message = messageRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ItemNotFoundException("Message not found"));

        messageRepository.deleteMessageById(message.getId());
        log.info("User {} message deleted {}", userId, id);
        return CommonResponse.success(new ResponseMessageDTO("Message deleted"));
    }
}
