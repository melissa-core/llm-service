package uz.melisa.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessagesDTO {

    private Long messageId;
    private String messageText;
    private String messageType;
    private String messageAuthorityType;
}
