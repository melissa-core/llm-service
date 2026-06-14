package uz.melisa.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.Chat;
import uz.melisa.dto.chat.ChatPageDTO;
import uz.melisa.enums.MessageAuthorityType;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long>, JpaSpecificationExecutor<Chat> {

    Optional<Chat> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    Optional<Chat> findTop1ByDeviceIdAndIsDeletedFalse(String deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Chat c where c.id = :id and c.isDeleted = false")
    Optional<Chat> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("update Chat set isTemporary = false, userId = :userId where id = :chatId")
    void activateChatByDeviceId(@Param("userId") Long userId, @Param("chatId") Long chatId);

    @Query(
            value = """
                      select new uz.melisa.dto.chat.ChatPageDTO(
                          c.id,
                          c.title,
                          firstUser.text,
                          c.createdAt
                      )
                      from Chat c
                      left join Message m
                             on m.chatId = c.id
                            and m.isDeleted = false
                    
                      left join Message firstUser
                             on firstUser.id = (
                                 select min(m1.id)
                                 from Message m1
                                 where m1.chatId = c.id
                                   and m1.isDeleted = false
                                   and m1.messageAuthorityType = :auth
                             )
                    
                      where c.userId = :userId
                        and c.isDeleted = false
                    
                      group by c.id, c.title, c.createdAt, c.updatedAt, firstUser.text
                    
                      order by coalesce(max(m.createdAt), c.updatedAt, c.createdAt) desc
                    """,
            countQuery = """
                      select count(c)
                      from Chat c
                      where c.userId = :userId
                        and c.isDeleted = false
                    """
    )
    Page<ChatPageDTO> findChatPagesOrdered(
            @Param("userId") Long userId,
            @Param("auth") MessageAuthorityType auth,
            Pageable pageable
    );
}
