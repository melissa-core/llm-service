package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.melisa.domain.Chat;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
}
