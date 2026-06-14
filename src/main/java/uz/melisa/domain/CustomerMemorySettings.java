package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_settings")
public class CustomerMemorySettings extends AuditingEntity {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Builder.Default
    @Column(name = "memory_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean memoryEnabled = false;

    @Builder.Default
    @Column(name = "generation", nullable = false, columnDefinition = "bigint default 1")
    private Long generation = 1L;

    @Builder.Default
    @Column(name = "fact_version", nullable = false, columnDefinition = "bigint default 1")
    private Long factVersion = 1L;

    @Column(name = "consent_at")
    private Timestamp consentAt;

    @Column(name = "opted_out_at")
    private Timestamp optedOutAt;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;
}
