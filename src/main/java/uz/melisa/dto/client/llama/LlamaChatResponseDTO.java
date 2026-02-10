package uz.melisa.dto.client.llama;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlamaChatResponseDTO {

    private String id;
    private String object;
    private long created;
    private String model;
    @JsonProperty("service_tier")
    private String serviceTier;
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
    @JsonProperty("prompt_logprobs")
    private String promptLogProbs;
    @JsonProperty("prompt_token_ids")
    private String promptTokenIds;
    @JsonProperty("kv_transfer_params")
    private String kvTransferParams;
    private List<LlamaResponseChoicesDTO> choices;
    private LlamaResponseChoiceUsageDTO usage;
}
