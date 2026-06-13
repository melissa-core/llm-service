package uz.melisa.dto.client.router;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummarizeMessageDTO {

    private String role;
    private String content;
}
