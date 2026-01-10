package uz.melisa.dto.claude;

import jakarta.validation.constraints.NotBlank;

public record ClaudeChatRequest(
        @NotBlank String message,
        Integer maxOutputTokens
) {}
