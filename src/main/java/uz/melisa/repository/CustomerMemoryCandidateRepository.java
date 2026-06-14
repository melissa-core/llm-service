package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemoryCandidate;
import uz.melisa.enums.MemoryCandidateStatus;
import uz.melisa.enums.MemoryFactType;

import java.sql.Timestamp;
import java.util.Optional;

public interface CustomerMemoryCandidateRepository extends JpaRepository<CustomerMemoryCandidate, Long> {

    @Query("""
            select c from CustomerMemoryCandidate c
            where c.customerId = :customerId
              and c.factType = :factType
              and c.factKey = :factKey
              and c.normalizedValue = :normalizedValue
              and c.status = :status
            """)
    Optional<CustomerMemoryCandidate> findMatchingCandidate(@Param("customerId") Long customerId,
                                                            @Param("factType") MemoryFactType factType,
                                                            @Param("factKey") String factKey,
                                                            @Param("normalizedValue") String normalizedValue,
                                                            @Param("status") MemoryCandidateStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerMemoryCandidate c
            set c.status = uz.melisa.enums.MemoryCandidateStatus.EXPIRED
            where c.status = uz.melisa.enums.MemoryCandidateStatus.PENDING
              and c.expiresAt < :now
            """)
    int expirePendingBefore(@Param("now") Timestamp now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerMemoryCandidate c
            set c.status = uz.melisa.enums.MemoryCandidateStatus.EXPIRED
            where c.status = uz.melisa.enums.MemoryCandidateStatus.PENDING
              and not exists (
                  select 1 from CustomerMemoryCandidateEvidence e
                  where e.candidateId = c.id and e.expiresAt > :now
              )
            """)
    int expirePendingWithoutActiveEvidence(@Param("now") Timestamp now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CustomerMemoryCandidate c where c.customerId = :customerId")
    int deleteByCustomerId(@Param("customerId") Long customerId);
}
