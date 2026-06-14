package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import uz.melisa.enums.MemoryFactSourceType;

import java.math.BigDecimal;
import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_candidate_evidence")
public class CustomerMemoryCandidateEvidence extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", insertable = false, updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CustomerMemoryCandidate candidate;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    // Soft reference only (no FK) so deleting an episode is never blocked by evidence rows.
    @Column(name = "episode_id")
    private Long episodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50, nullable = false)
    private MemoryFactSourceType sourceType;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;
}
