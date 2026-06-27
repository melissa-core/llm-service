package uz.melisa.service.impl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.melisa.domain.MessageProductSuggestion;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.product.ProductSuggestionDTO;
import uz.melisa.repository.MessageProductSuggestionRepository;
import uz.melisa.service.MessageProductSuggestionService;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProductSuggestionServiceImpl implements MessageProductSuggestionService {

    private final MessageProductSuggestionRepository messageProductSuggestionRepository;

    @Override
    @CacheEvict(value = "llm:message-product-suggestions", key = "#messageId")
    public void saveProductSuggestion(long messageId, List<Long> productIds) {
        List<MessageProductSuggestion> list = new ArrayList<>();
        for (int id = 0; id < productIds.size(); id++) {
            MessageProductSuggestion messageProductSuggestion = new MessageProductSuggestion();
            messageProductSuggestion.setMessageId(messageId);
            messageProductSuggestion.setProductId(productIds.get(id));
            messageProductSuggestion.setPosition(id + 1);
            list.add(messageProductSuggestion);
        }
        messageProductSuggestionRepository.saveAll(list);
    }

    @Override
    @Cacheable(value = "llm:message-product-suggestions", key = "#id")
    public CommonResponse<List<ProductSuggestionDTO>> getProductsByMessage(Long id) {
        List<MessageProductSuggestion> products = messageProductSuggestionRepository.findAllByMessageId(id);
        if (products.isEmpty()) return CommonResponse.success(new ArrayList<>());

        return CommonResponse.success(products.stream().map(product -> new ProductSuggestionDTO(
                        product.getProductId(), product.getPosition()
                ))
                .toList());
    }
}
