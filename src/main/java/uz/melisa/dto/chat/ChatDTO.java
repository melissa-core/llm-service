package uz.melisa.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {

    private Long id;
    private String title;
    private Timestamp createdAt;
}
