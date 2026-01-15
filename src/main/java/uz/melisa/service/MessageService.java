package uz.melisa.service;

import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;

public interface MessageService {

    CommonResponse<MessageResponseDTO> sendMessage(MessageSendRequestDTO messageSendRequestDTO);
}
