package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Chat;
import uz.melisa.domain.Message;
import uz.melisa.exp.BadRequestException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;

import java.util.Map;

import static uz.melisa.util.ChatUtil.buildChatByDeviceId;
import static uz.melisa.util.MessageUtil.buildModelMessage;
import static uz.melisa.util.MessageUtil.buildUserMessage;
import static uz.melisa.util.StringUtil.trimToEmpty;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestMessageHelperService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public Chat saveOrGetGuestChat(String deviceId, String messageText) {
        String device = trimToEmpty(deviceId);
        if (device.isEmpty()) throw new BadRequestException("Device id cannot be null or empty");

        String text = trimToEmpty(messageText);
        if (text.isEmpty()) throw new BadRequestException("Message cannot be null or empty");

        return chatRepository.findTop1ByDeviceIdAndIsDeletedFalse(device)
                .orElseGet(() -> {
                    Chat created = chatRepository.save(buildChatByDeviceId(device, text));
                    chatRepository.flush();
                    return created;
                });
    }

    @Transactional
    public Message saveGuestUserMessage(Long chatId, String messageText) {
        Message userMessage = messageRepository.save(buildUserMessage(messageText, chatId, null));
        messageRepository.flush();
        return userMessage;
    }

    @Transactional
    public Message saveGuestModelMessage(Long chatId, Map<Boolean, String> modelResponse) {
        Map.Entry<Boolean, String> entry = modelResponse.entrySet().iterator().next();
        Boolean hasSuggestions = entry.getKey();
        String modelText = entry.getValue();
        Message modelMessage = messageRepository.save(buildModelMessage(modelText, hasSuggestions, chatId, null));
        messageRepository.flush();
        return modelMessage;
    }
}
