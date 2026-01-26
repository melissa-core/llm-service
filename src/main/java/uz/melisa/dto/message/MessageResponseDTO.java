package uz.melisa.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponseDTO {

    private String message;
    private Long chatId;
    private Long userMessageId;
    private Long modelMessageId;
}
