package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.melisa.domain.CustomerMemoryDeletionOutbox;
import uz.melisa.enums.MemoryDeletionOutboxStatus;

import java.util.List;

public interface CustomerMemoryDeletionOutboxRepository extends JpaRepository<CustomerMemoryDeletionOutbox, Long> {

    List<CustomerMemoryDeletionOutbox> findAllByStatus(MemoryDeletionOutboxStatus status);
}
