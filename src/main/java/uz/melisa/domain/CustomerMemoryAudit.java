package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.melisa.enums.MemoryAuditAction;
import uz.melisa.enums.MemoryFactSourceType;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_audit")
public class CustomerMemoryAudit extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fact_id", insertable = false, updatable = false)
    private CustomerMemoryFact fact;

    @Column(name = "fact_id")
    private Long factId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50, nullable = false)
    private MemoryAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50)
    private MemoryFactSourceType sourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata;
}
