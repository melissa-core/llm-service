package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.enums.MessageModelType;
import uz.melisa.enums.MessageType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "message")
@Builder
public class Message extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", insertable = false, updatable = false, nullable = false)
    private Chat chat;

    @Column(name = "chat_id")
    private Long chatId;

    @Enumerated(EnumType.STRING)
    private MessageAuthorityType messageAuthorityType;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    private MessageModelType messageModelType;

    private Long userId;

    private boolean isDeleted;
}
