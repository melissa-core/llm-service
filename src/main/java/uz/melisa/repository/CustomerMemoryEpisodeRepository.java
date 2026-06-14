package uz.melisa.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemoryEpisode;

import java.util.List;
import java.util.Optional;

public interface CustomerMemoryEpisodeRepository extends JpaRepository<CustomerMemoryEpisode, Long> {

    Optional<CustomerMemoryEpisode> findByChatIdAndSegmentNumber(Long chatId, Integer segmentNumber);

    @Query("select e.summary from CustomerMemoryEpisode e where e.chatId = :chatId order by e.segmentNumber desc")
    List<String> findRecentSummaries(@Param("chatId") Long chatId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CustomerMemoryEpisode e where e.customerId = :customerId")
    int deleteByCustomerId(@Param("customerId") Long customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CustomerMemoryEpisode e where e.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") java.sql.Timestamp now);
}
