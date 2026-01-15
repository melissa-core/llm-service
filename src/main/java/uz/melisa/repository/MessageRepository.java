package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.melisa.domain.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
