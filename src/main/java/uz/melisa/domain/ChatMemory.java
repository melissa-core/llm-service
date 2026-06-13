package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "chat_memory")
@Builder
public class ChatMemory extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", insertable = false, updatable = false, nullable = false)
    private Chat chat;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(columnDefinition = "TEXT")
    private String summary;
}
