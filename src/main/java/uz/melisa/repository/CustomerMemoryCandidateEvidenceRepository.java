package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemoryCandidateEvidence;

import java.sql.Timestamp;
import java.util.Optional;

public interface CustomerMemoryCandidateEvidenceRepository extends JpaRepository<CustomerMemoryCandidateEvidence, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CustomerMemoryCandidateEvidence e where e.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") Timestamp now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from CustomerMemoryCandidateEvidence e
            where e.candidateId in (select c.id from CustomerMemoryCandidate c where c.customerId = :customerId)
            """)
    int deleteByCustomerId(@Param("customerId") Long customerId);

    Optional<CustomerMemoryCandidateEvidence> findFirstByCandidateIdAndChatIdOrderByIdAsc(Long candidateId, Long chatId);

    @Query("""
            select count(distinct e.chatId) from CustomerMemoryCandidateEvidence e
            where e.candidateId = :candidateId
              and e.expiresAt > :now
            """)
    long countDistinctChatsByCandidateIdAndNotExpired(@Param("candidateId") Long candidateId,
                                                      @Param("now") Timestamp now);
}
