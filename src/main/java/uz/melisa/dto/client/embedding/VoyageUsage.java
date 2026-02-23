package uz.melisa.dto.client.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageUsage {

    @JsonProperty("total_tokens")
    private long totalTokens;
}
