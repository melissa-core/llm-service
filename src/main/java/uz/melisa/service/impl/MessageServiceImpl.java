package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.WordUtils;
import org.springframework.stereotype.Service;
import uz.melisa.domain.Chat;
import uz.melisa.domain.Message;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.enums.MessageModelType;
import uz.melisa.enums.MessageType;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.ClaudeChatService;
import uz.melisa.service.MessageService;

import static uz.melisa.util.SecurityUtil.getCurrentUserId;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ClaudeChatService claudeChatService;

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
        message = messageRepository.save(buildUserMessage(messageSendRequestDTO, chat.getId(), userId));
        String modelResponseMessage = claudeChatService.chatUzbek(message.getText());
        Message modelMessageResponse = messageRepository.save(buildModelMessage(modelResponseMessage, message.getChatId(), userId));
        return CommonResponse.success(new MessageResponseDTO(modelMessageResponse.getText(), chat.getId()));
    }

    private Chat buildChat(Long userId, String chatTitle) {
        Chat chat = new Chat();
        chat.setUserId(userId);
        chat.setTitle(chatTitle);
        return chat;
    }

    private Message buildModelMessage(String textMessage, Long chatId, Long userId) {
        Message message = new Message();
        message.setChatId(chatId);
        message.setUserId(userId);
        message.setText(textMessage);
        message.setMessageType(MessageType.TEXT);
        message.setMessageAuthorityType(MessageAuthorityType.MODEL);
        message.setMessageModelType(MessageModelType.CLAUDE);
        return message;
    }

    private Message buildUserMessage(MessageSendRequestDTO messageSendRequestDTO, Long chatId, Long userId) {
        Message message = new Message();
        message.setMessageType(MessageType.TEXT);
        message.setMessageAuthorityType(MessageAuthorityType.USER);
        message.setChatId(chatId);
        message.setUserId(userId);
        message.setText(messageSendRequestDTO.getMessage());
        return message;
    }

    private String getChatTitle(String message) {
        String title = message.length() > 11
                ? message.substring(0, 11) + "..."
                : message;

        return WordUtils.capitalizeFully(title);
    }
}
