package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uz.melisa.domain.ChatMemory;

import java.util.Optional;

public interface ChatMemoryRepository extends JpaRepository<ChatMemory, Long>, JpaSpecificationExecutor<ChatMemory> {

    Optional<ChatMemory> findByChatId(Long chatId);
}
