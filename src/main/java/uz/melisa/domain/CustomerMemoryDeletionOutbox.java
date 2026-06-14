package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import uz.melisa.enums.MemoryDeletionOutboxStatus;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_deletion_outbox")
public class CustomerMemoryDeletionOutbox extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "generation", nullable = false)
    private Long generation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private MemoryDeletionOutboxStatus status;

    @Builder.Default
    @Column(name = "attempts", nullable = false, columnDefinition = "integer default 0")
    private Integer attempts = 0;

    @Column(name = "next_retry_at")
    private Timestamp nextRetryAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
}
