package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Chat;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.claude.ClaudeChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;
import uz.melisa.exp.BadRequestException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.ClaudeChatService;
import uz.melisa.service.GuestService;

import static uz.melisa.util.ChatUtil.buildChatByDeviceId;
import static uz.melisa.util.MessageUtil.buildModelMessage;
import static uz.melisa.util.MessageUtil.buildUserMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {

    private final ClaudeChatService claudeChatService;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Transactional
    @Override
    public CommonResponse<GuestMessageResponseDTO> guestSendMessage(String deviceId, ClaudeChatRequest request) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BadRequestException("Device id cannot be null or empty");
        }

        String messageText = request.getMessage();
        Chat chat = chatRepository.findTop1ByDeviceIdAndIsDeletedFalse(deviceId)
                .orElseGet(() -> chatRepository.save(buildChatByDeviceId(deviceId, messageText)));

        messageRepository.save(buildUserMessage(messageText, chat.getId(), null));
        String claudeResponse = claudeChatService.chatWithClaude(messageText);
        messageRepository.save(buildModelMessage(claudeResponse, chat.getId(), null));
        return CommonResponse.success(new GuestMessageResponseDTO(claudeResponse));
    }

    @Transactional(readOnly = true)
    @Override
    public CommonResponse<Page<ChatMessagesDTO>> getMessages(String deviceId, Pageable pageable) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BadRequestException("Device id cannot be null or empty");
        }
        Chat chat = chatRepository.findTop1ByDeviceIdAndIsDeletedFalse(deviceId)
                .orElse(null);

        if (chat == null) {
            return CommonResponse.success(Page.empty());
        }
        Page<ChatMessagesDTO> pageMessages = messageRepository.findGuestChatMessages(chat.getId(), pageable);
        return CommonResponse.success(pageMessages);
    }
}
