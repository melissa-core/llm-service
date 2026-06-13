package uz.melisa.dto.client.llama;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlamaResponseChoiceMessageDTO {

    private String role;
    private String content;
}
