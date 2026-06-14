package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.melisa.enums.MemoryEpisodeSentiment;

import java.sql.Timestamp;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "customer_memory_episode")
public class CustomerMemoryEpisode extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "segment_number", nullable = false)
    private Integer segmentNumber;

    @Column(name = "summarized_from_message_seq", nullable = false)
    private Long summarizedFromMessageSeq;

    @Column(name = "summarized_until_message_seq", nullable = false)
    private Long summarizedUntilMessageSeq;

    @Column(name = "summary", columnDefinition = "text", nullable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topics", columnDefinition = "jsonb", nullable = false)
    private List<String> topics;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 30, nullable = false)
    private MemoryEpisodeSentiment sentiment;

    @Column(name = "summarization_version", length = 50, nullable = false)
    private String summarizationVersion;

    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;
}
