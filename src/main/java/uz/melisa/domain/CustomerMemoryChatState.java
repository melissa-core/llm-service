package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_chat_state")
public class CustomerMemoryChatState extends AuditingEntity {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Builder.Default
    @Column(name = "last_completed_message_seq", nullable = false, columnDefinition = "bigint default 0")
    private Long lastCompletedMessageSeq = 0L;

    @Builder.Default
    @Column(name = "next_segment_number", nullable = false, columnDefinition = "integer default 1")
    private Integer nextSegmentNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_job_id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private CustomerMemoryProcessingJob activeJob;

    @Column(name = "active_job_id")
    private Long activeJobId;

    @Builder.Default
    @Column(name = "poison_attempts", nullable = false, columnDefinition = "integer default 0")
    private Integer poisonAttempts = 0;
}
