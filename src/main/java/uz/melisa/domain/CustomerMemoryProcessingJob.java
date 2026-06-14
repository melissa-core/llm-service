package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import uz.melisa.enums.MemoryProcessingJobStatus;

import java.sql.Timestamp;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_processing_job")
public class CustomerMemoryProcessingJob extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", length = 300, nullable = false)
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "segment_number", nullable = false)
    private Integer segmentNumber;

    @Column(name = "from_message_seq", nullable = false)
    private Long fromMessageSeq;

    @Column(name = "until_message_seq", nullable = false)
    private Long untilMessageSeq;

    @Column(name = "customer_memory_generation", nullable = false)
    private Long customerMemoryGeneration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private MemoryProcessingJobStatus status;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "lease_token", columnDefinition = "uuid")
    private UUID leaseToken;

    @Column(name = "locked_until")
    private Timestamp lockedUntil;

    @Builder.Default
    @Column(name = "attempts", nullable = false, columnDefinition = "integer default 0")
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "next_retry_at")
    private Timestamp nextRetryAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
}
