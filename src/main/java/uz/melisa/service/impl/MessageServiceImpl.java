package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.Message;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.chat.ChatUserMessageDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.MessageRepository;
import uz.melisa.service.GlobalMessageHandler;
import uz.melisa.service.MessageService;

import static uz.melisa.util.SecurityUtil.getCurrentUserId;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageHelperService messageHelperService;
    private final MessageRepository messageRepository;
    private final GlobalMessageHandler globalMessageHandler;

    @Override
    public CommonResponse<MessageResponseDTO> sendMessage(MessageSendRequestDTO req) {
        Long userId = getCurrentUserId();

        ChatUserMessageDTO chatUserMessage = messageHelperService.saveChatAndUserMessage(req, userId);

        String modelText = globalMessageHandler.handleChatMessage(chatUserMessage);

        Message modelMessage = messageHelperService.saveModelMessage(chatUserMessage.getChatId(), userId, modelText);

        return CommonResponse.success(
                new MessageResponseDTO(modelMessage.getText(), chatUserMessage.getChatId(), chatUserMessage.getMessageId(), modelMessage.getId())
        );
    }

    @Transactional
    @Override
    public CommonResponse<ResponseMessageDTO> deleteMessage(long id) {
        Long userId = getCurrentUserId();
        Message message = messageRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ItemNotFoundException("Message not found"));

        messageRepository.deleteMessageById(message.getId());
        log.info("User {} message deleted {}", userId, id);
        return CommonResponse.success(new ResponseMessageDTO("Message deleted"));
    }
}
