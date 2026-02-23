package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.EmbeddingDetails;
import uz.melisa.dto.client.embedding.VoyageEmbeddingData;
import uz.melisa.dto.client.embedding.VoyageEmbeddingResponseDTO;
import uz.melisa.repository.EmbeddingDetailsRepository;
import uz.melisa.service.EmbeddingService;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingDetailsRepository embeddingDetailsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void saveEmbedding(Long messageId, VoyageEmbeddingResponseDTO voyageEmbeddingResponseDTO,
                              String message) {
        for (VoyageEmbeddingData data : voyageEmbeddingResponseDTO.getData()) {
            EmbeddingDetails embeddingDetails = new EmbeddingDetails();
            embeddingDetails.setModel(voyageEmbeddingResponseDTO.getModel());
            embeddingDetails.setTotalTokens(voyageEmbeddingResponseDTO.getUsage().getTotalTokens());
            embeddingDetails.setMessageId(messageId);
            embeddingDetails.setEmbeddingIndex(data.getIndex());
            embeddingDetails.setEmbedding(data.getEmbedding());
            embeddingDetails.setInput(message);
            embeddingDetailsRepository.save(embeddingDetails);
        }
    }
}
