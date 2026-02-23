package uz.melisa.service;

import uz.melisa.dto.client.embedding.VoyageEmbeddingResponseDTO;

public interface EmbeddingService {

    void saveEmbedding(Long messageId, VoyageEmbeddingResponseDTO voyageEmbeddingResponseDTO,
                       String message);
}
