package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.ChatMemory;

import java.util.Optional;

public interface ChatMemoryRepository extends JpaRepository<ChatMemory, Long>, JpaSpecificationExecutor<ChatMemory> {

    @Transactional(readOnly = true)
    Optional<ChatMemory> findByChatId(Long chatId);
}
