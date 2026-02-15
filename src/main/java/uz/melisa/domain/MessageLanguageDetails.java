package uz.melisa.domain;

import jakarta.persistence.*;
import lombok.*;
import uz.melisa.enums.MessageAuthorityType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "message_language_details")
@Builder
public class MessageLanguageDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", insertable = false, updatable = false)
    private Chat chat;

    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private Message message;

    @Column(name = "message_id")
    private Long messageId;

    @Enumerated(EnumType.STRING)
    private MessageAuthorityType messageAuthorityType;

    private String detectedLanguage;

    private String detectedScript;

    private String text;
}
