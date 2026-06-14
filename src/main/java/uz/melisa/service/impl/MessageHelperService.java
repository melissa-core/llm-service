package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Chat;
import uz.melisa.domain.Message;
import uz.melisa.dto.chat.ChatUserMessageDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;

import static uz.melisa.util.ChatUtil.buildChat;
import static uz.melisa.util.ChatUtil.getChatTitle;
import static uz.melisa.util.MessageUtil.buildModelMessage;
import static uz.melisa.util.MessageUtil.buildUserMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageHelperService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageSequenceService messageSequenceService;

    @Transactional
    public ChatUserMessageDTO saveChatAndUserMessage(MessageSendRequestDTO req, Long userId) {
        Chat chat;
        if (req.getChatId() != null) {
            chat = chatRepository.findByIdAndUserIdAndIsDeletedFalse(req.getChatId(), userId)
                    .orElseThrow(() -> new ItemNotFoundException("Chat not found"));
        } else {
            chat = chatRepository.save(buildChat(userId, getChatTitle(req.getMessage())));
            chatRepository.flush();
        }

        Message userMessage = buildUserMessage(req.getMessage(), chat.getId(), userId);
        userMessage.setMessageSeq(messageSequenceService.nextMessageSeq(chat.getId()));
        userMessage = messageRepository.save(userMessage);
        messageRepository.flush();

        return new ChatUserMessageDTO(chat.getId(), userMessage.getId(), userMessage.getText());
    }

    @Transactional
    public Message saveModelMessage(Long chatId, Long userId, String modelText, Boolean hasSuggestions) {
        Message modelMessage = buildModelMessage(modelText, hasSuggestions, chatId, userId);
        modelMessage.setMessageSeq(messageSequenceService.nextMessageSeq(chatId));
        modelMessage = messageRepository.save(modelMessage);
        messageRepository.flush();
        return modelMessage;
    }
}
