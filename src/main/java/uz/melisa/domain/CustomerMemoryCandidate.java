package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.melisa.enums.MemoryCandidateStatus;
import uz.melisa.enums.MemoryFactType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_candidate")
public class CustomerMemoryCandidate extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fact_type", length = 50, nullable = false)
    private MemoryFactType factType;

    @Column(name = "fact_key", length = 100, nullable = false)
    private String factKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> valueJson;

    @Column(name = "normalized_value", length = 500, nullable = false)
    private String normalizedValue;

    @Column(name = "max_confidence", precision = 5, scale = 4)
    private BigDecimal maxConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private MemoryCandidateStatus status;

    @Column(name = "first_seen_at", nullable = false)
    private Timestamp firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Timestamp lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoted_fact_id", insertable = false, updatable = false)
    private CustomerMemoryFact promotedFact;

    @Column(name = "promoted_fact_id")
    private Long promotedFactId;
}
