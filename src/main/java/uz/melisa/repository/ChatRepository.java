package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uz.melisa.domain.Chat;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long>, JpaSpecificationExecutor<Chat> {

    Optional<Chat> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
}
