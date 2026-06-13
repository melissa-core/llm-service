package uz.melisa.service;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;

public interface MessageService {

    CommonResponse<MessageResponseDTO> sendMessage(MessageSendRequestDTO messageSendRequestDTO);

    Flux<ServerSentEvent<Object>> sendMessageStream(MessageSendRequestDTO messageSendRequestDTO);

    CommonResponse<ResponseMessageDTO> deleteMessage(long id);
}
