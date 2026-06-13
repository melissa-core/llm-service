package uz.melisa.dto.message;

import uz.melisa.enums.AiRoute;

import java.util.List;

/**
 * Pre-computed inputs for a streamed chat completion: which model route to
 * call, the conversation memory key, the fully built model input, and the
 * product context resolved before the stream starts.
 */
public record ChatStreamPlan(
        AiRoute route,
        String conversationKey,
        String modelInput,
        List<Long> productIds,
        boolean productBased
) {
}
