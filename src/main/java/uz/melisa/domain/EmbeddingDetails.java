package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "embedding_details")
@Builder
public class EmbeddingDetails extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String model;

    @Column(columnDefinition = "text")
    private String input;

    private String inputType;

    private long totalTokens;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private float[] embeddings;

    private Integer embeddingIndex;

    private long messageId;
}
