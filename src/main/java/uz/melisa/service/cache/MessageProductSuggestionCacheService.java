package uz.melisa.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uz.melisa.dto.product.ProductSuggestionDTO;
import uz.melisa.service.impl.MessageProductSuggestionServiceImpl;

import java.util.List;

/**
 * Cache producer for a message's product suggestions.
 * Cached value is a plain {@code List<ProductSuggestionDTO>} (never a {@code CommonResponse} wrapper),
 * keyed by message id to match the {@code @CacheEvict(key = "#messageId")} on the writer.
 */
@Service
@Slf4j
public class MessageProductSuggestionCacheService {

    @Lazy
    @Autowired
    private MessageProductSuggestionServiceImpl messageProductSuggestionServiceImpl;

    @Cacheable(value = "llm:message-product-suggestions", key = "#id")
    public List<ProductSuggestionDTO> getProductsByMessageData(Long id) {
        return messageProductSuggestionServiceImpl.loadProductsByMessageData(id);
    }
}
