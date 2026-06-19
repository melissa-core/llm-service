package uz.melisa.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Summary view of a chat conversation shown in a paginated chat list")
public class ChatPageDTO {

    @Schema(description = "Unique identifier of the chat", example = "1024")
    private Long id;

    @Schema(description = "Title of the chat conversation", example = "Order help")
    private String title;

    @Schema(description = "Short preview text shown under the chat title", example = "Where is my order?")
    private String subtitle;

    @Schema(description = "Timestamp when the chat was created", example = "2026-06-19T14:30:00.000+05:00")
    private Timestamp createdAt;
}
