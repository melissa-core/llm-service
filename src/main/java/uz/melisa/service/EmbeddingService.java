package uz.melisa.service;

import uz.melisa.dto.client.embedding.OpenAiEmbeddingResponseDTO;

public interface EmbeddingService {

    void saveEmbedding(Long messageId, OpenAiEmbeddingResponseDTO openAiEmbeddingResponseDTO,
                       String message);
}
