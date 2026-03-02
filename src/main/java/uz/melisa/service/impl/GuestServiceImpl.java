package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Chat;
import uz.melisa.domain.Message;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.chat.ChatUserMessageDTO;
import uz.melisa.dto.claude.ClaudeChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;
import uz.melisa.exp.BadRequestException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.GlobalMessageHandler;
import uz.melisa.service.GuestService;

import java.util.Map;

import static uz.melisa.util.StringUtil.trimToEmpty;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {

    private final GlobalMessageHandler globalMessageHandler;
    private final GuestMessageHelperService guestMessageHelperService;

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Override
    public CommonResponse<GuestMessageResponseDTO> guestSendMessage(String deviceId, ClaudeChatRequest request) {
        String device = trimToEmpty(deviceId);
        if (device.isEmpty()) throw new BadRequestException("Device id cannot be null or empty");

        String messageText = request == null ? "" : trimToEmpty(request.getMessage());
        if (messageText.isEmpty()) throw new BadRequestException("Message cannot be null or empty");

        Chat chat = guestMessageHelperService.saveOrGetGuestChat(device, messageText);
        Message message = guestMessageHelperService.saveGuestUserMessage(chat.getId(), messageText);

        Map<Boolean, String> modelResponse = globalMessageHandler.handleChatMessage(new ChatUserMessageDTO(
                chat.getId(), message.getId(), messageText
        ));

        Message savedMessage = guestMessageHelperService.saveGuestModelMessage(chat.getId(), modelResponse);

        return CommonResponse.success(new GuestMessageResponseDTO(savedMessage.getText()));
    }

    @Transactional(readOnly = true)
    @Override
    public CommonResponse<Page<ChatMessagesDTO>> getMessages(String deviceId, Pageable pageable) {
        String device = trimToEmpty(deviceId);
        if (device.isEmpty()) throw new BadRequestException("Device id cannot be null or empty");

        Chat chat = chatRepository.findTop1ByDeviceIdAndIsDeletedFalse(device).orElse(null);
        if (chat == null) return CommonResponse.success(Page.empty());

        Page<ChatMessagesDTO> pageMessages = messageRepository.findGuestChatMessages(chat.getId(), pageable);
        return CommonResponse.success(pageMessages);
    }
}
