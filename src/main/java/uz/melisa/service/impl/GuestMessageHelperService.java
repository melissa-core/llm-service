package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Chat;
import uz.melisa.domain.Message;
import uz.melisa.dto.message.ProductBasedMessage;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;

import java.util.Map;

import static uz.melisa.util.ChatUtil.buildChatByDeviceId;
import static uz.melisa.util.MessageUtil.buildModelMessage;
import static uz.melisa.util.MessageUtil.buildUserMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestMessageHelperService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final MessageSequenceService messageSequenceService;

    @Transactional
    public Chat saveOrGetGuestChat(String deviceId, String messageText) {
        return chatRepository.findTop1ByDeviceIdAndIsDeletedFalse(deviceId)
                .orElseGet(() -> {
                    Chat created = chatRepository.save(buildChatByDeviceId(deviceId, messageText));
                    chatRepository.flush();
                    return created;
                });
    }

    @Transactional
    public Message saveGuestUserMessage(Long chatId, String messageText) {
        Message userMessage = buildUserMessage(messageText, chatId, null);
        userMessage.setMessageSeq(messageSequenceService.nextMessageSeq(chatId));
        userMessage = messageRepository.save(userMessage);
        messageRepository.flush();
        return userMessage;
    }

    @Transactional
    public Message saveGuestModelMessage(Long chatId, Map<Boolean, ProductBasedMessage> modelResponse) {
        Map.Entry<Boolean, ProductBasedMessage> entry = modelResponse.entrySet().iterator().next();
        Boolean hasSuggestions = entry.getKey();
        String modelText = entry.getValue().getMessage();
        Message modelMessage = buildModelMessage(modelText, hasSuggestions, chatId, null);
        modelMessage.setMessageSeq(messageSequenceService.nextMessageSeq(chatId));
        modelMessage = messageRepository.save(modelMessage);
        messageRepository.flush();
        return modelMessage;
    }
}
