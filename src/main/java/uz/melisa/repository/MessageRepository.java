package uz.melisa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.Message;
import uz.melisa.dto.chat.ChatMessagesDTO;

public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    @Query("""
            select new uz.melisa.dto.chat.ChatMessagesDTO(
                m.id,
                m.text,
                cast(m.messageType as string),
                cast(m.messageAuthorityType as string)
            )
            from Message m
            where m.chatId = :chatId
              and m.userId = :userId
              and m.isDeleted = false
            """)
    Page<ChatMessagesDTO> findChatMessages(@Param("chatId") Long chatId,
                                           @Param("userId") Long userId,
                                           Pageable pageable);
}
