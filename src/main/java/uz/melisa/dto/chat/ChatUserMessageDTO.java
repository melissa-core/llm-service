package uz.melisa.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatUserMessageDTO {

    private Long chatId;
    private Long messageId;
    private String message;
}
