package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.melisa.domain.MessageProductSuggestion;

import java.util.List;

public interface MessageProductSuggestionRepository extends JpaRepository<MessageProductSuggestion, Long> {

    List<MessageProductSuggestion> findAllByMessageId(Long messageId);
}
