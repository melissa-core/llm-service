package uz.melisa.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uz.melisa.domain.MessageProductSuggestion;
import uz.melisa.dto.product.ProductSuggestionDTO;
import uz.melisa.repository.MessageProductSuggestionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache producer for a message's product suggestions.
 * Cached value is a plain {@code List<ProductSuggestionDTO>} (never a {@code CommonResponse} wrapper),
 * keyed by message id to match the {@code @CacheEvict(key = "#messageId")} on the writer.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProductSuggestionCacheService {

    private final MessageProductSuggestionRepository messageProductSuggestionRepository;

    @Cacheable(value = "llm:message-product-suggestions", key = "#id")
    public List<ProductSuggestionDTO> getProductsByMessageData(Long id) {
        List<MessageProductSuggestion> products = messageProductSuggestionRepository.findAllByMessageId(id);
        if (products.isEmpty()) return new ArrayList<>();

        return products.stream().map(product -> new ProductSuggestionDTO(
                        product.getProductId(), product.getPosition()
                ))
                .toList();
    }
}
