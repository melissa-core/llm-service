package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.melisa.enums.MemoryFactCardinality;
import uz.melisa.enums.MemoryFactSourceType;
import uz.melisa.enums.MemoryFactStatus;
import uz.melisa.enums.MemoryFactType;
import uz.melisa.enums.MemoryRiskClass;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_fact")
public class CustomerMemoryFact extends AuditingEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "cardinality", length = 30, nullable = false)
    private MemoryFactCardinality cardinality;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_class", length = 30, nullable = false)
    private MemoryRiskClass riskClass;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> valueJson;

    @Column(name = "normalized_value", length = 500, nullable = false)
    private String normalizedValue;

    @Column(name = "severity", length = 30)
    private String severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50, nullable = false)
    private MemoryFactSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private MemoryFactStatus status;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Builder.Default
    @Column(name = "schema_version", nullable = false, columnDefinition = "integer default 1")
    private Integer schemaVersion = 1;

    @Column(name = "first_seen_at", nullable = false)
    private Timestamp firstSeenAt;

    @Column(name = "last_confirmed_at")
    private Timestamp lastConfirmedAt;
}
