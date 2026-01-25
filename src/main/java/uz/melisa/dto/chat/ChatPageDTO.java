package uz.melisa.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatPageDTO {

    private Long id;
    private String title;
    private String subtitle;
    private Timestamp createdAt;
}
