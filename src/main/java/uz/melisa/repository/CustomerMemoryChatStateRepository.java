package uz.melisa.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemoryChatState;

import java.util.List;
import java.util.Optional;

public interface CustomerMemoryChatStateRepository extends JpaRepository<CustomerMemoryChatState, Long> {

    List<CustomerMemoryChatState> findAllByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CustomerMemoryChatState s where s.chatId = :chatId")
    Optional<CustomerMemoryChatState> findByChatIdForUpdate(@Param("chatId") Long chatId);

    @Query("""
            select s.chatId from CustomerMemoryChatState s
            where exists (
                select 1 from Message m
                where m.chatId = s.chatId
                  and m.isDeleted = false
                  and m.messageSeq > s.lastCompletedMessageSeq
            )
            """)
    List<Long> findChatIdsWithPendingMessages();

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CustomerMemoryChatState s set s.activeJobId = null where s.customerId = :customerId")
    int clearActiveJobsByCustomerId(@Param("customerId") Long customerId);
}
