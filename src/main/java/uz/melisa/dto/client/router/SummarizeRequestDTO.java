package uz.melisa.dto.client.router;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummarizeRequestDTO {

    @JsonProperty("max_tokens")
    private int maxTokens;
    @JsonProperty("prev_summary")
    private String prevSummary;
    private List<SummarizeMessageDTO> messages;
}
