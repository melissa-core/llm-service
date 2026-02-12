package uz.melisa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.Message;
import uz.melisa.dto.chat.ChatMessagesDTO;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {

    @Query("""
            select new uz.melisa.dto.chat.ChatMessagesDTO(
                m.id,
                m.text,
                cast(m.messageType as string),
                cast(m.messageAuthorityType as string),
                m.createdAt
            )
            from Message m
            where m.chatId = :chatId
              and m.userId = :userId
              and m.isDeleted = false
            """)
    Page<ChatMessagesDTO> findChatMessages(@Param("chatId") Long chatId,
                                           @Param("userId") Long userId,
                                           Pageable pageable);

    @Query("""
            select new uz.melisa.dto.chat.ChatMessagesDTO(
                m.id,
                m.text,
                cast(m.messageType as string),
                cast(m.messageAuthorityType as string),
                m.createdAt
            )
            from Message m
            where m.chatId = :chatId
              and m.isDeleted = false
            """)
    Page<ChatMessagesDTO> findGuestChatMessages(@Param("chatId") Long chatId,
                                                Pageable pageable);

    Optional<Message> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<Message> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    @Modifying
    @Query("update Message set isDeleted = true where id = :id")
    void deleteMessageById(@Param("id") Long id);

    @Modifying
    @Query("update Message set userId = :userId where chatId = :chatId")
    void setUserId(@Param("userId") Long userId,
                   @Param("chatId") Long chatId);
}
