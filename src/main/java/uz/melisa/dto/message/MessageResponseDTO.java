package uz.melisa.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.melisa.enums.MessageContentType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponseDTO {

    private String message;
    private Long chatId;
    private Long userMessageId;
    private Long modelMessageId;
    private MessageContentType contentType;
}
