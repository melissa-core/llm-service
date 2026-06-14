package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemoryProcessingJob;
import uz.melisa.enums.MemoryProcessingJobStatus;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerMemoryProcessingJobRepository extends JpaRepository<CustomerMemoryProcessingJob, Long> {

    Optional<CustomerMemoryProcessingJob> findByIdempotencyKey(String idempotencyKey);

    List<CustomerMemoryProcessingJob> findAllByStatus(MemoryProcessingJobStatus status);

    Optional<CustomerMemoryProcessingJob> findFirstByChatIdAndFromMessageSeqOrderByIdDesc(Long chatId, long fromMessageSeq);

    @Query("""
            select j from CustomerMemoryProcessingJob j
            where j.status = :status
              and j.lockedUntil is not null
              and j.lockedUntil < :now
            """)
    List<CustomerMemoryProcessingJob> findExpiredLeases(@Param("status") MemoryProcessingJobStatus status,
                                                        @Param("now") Timestamp now);

    /** Atomic lease (re)claim. Returns 1 when this row was reclaimable and is now leased, else 0. */
    @Modifying
    @Query("""
            update CustomerMemoryProcessingJob j
            set j.status = uz.melisa.enums.MemoryProcessingJobStatus.IN_PROGRESS,
                j.workerId = :workerId,
                j.leaseToken = :leaseToken,
                j.lockedUntil = :lockedUntil,
                j.attempts = j.attempts + 1,
                j.nextRetryAt = null,
                j.customerMemoryGeneration = :generation
            where j.id = :id
              and j.status in (uz.melisa.enums.MemoryProcessingJobStatus.FAILED, uz.melisa.enums.MemoryProcessingJobStatus.IN_PROGRESS)
              and j.attempts < j.maxAttempts
              and (j.nextRetryAt is null or j.nextRetryAt <= :now)
              and (j.lockedUntil is null or j.lockedUntil <= :now)
            """)
    int reclaimLease(@Param("id") Long id,
                     @Param("workerId") String workerId,
                     @Param("leaseToken") UUID leaseToken,
                     @Param("lockedUntil") Timestamp lockedUntil,
                     @Param("generation") long generation,
                     @Param("now") Timestamp now);

    /** Lease heartbeat. Returns 1 only while this worker still holds the IN_PROGRESS lease. */
    @Modifying
    @Query("""
            update CustomerMemoryProcessingJob j
            set j.lockedUntil = :lockedUntil
            where j.id = :id
              and j.leaseToken = :leaseToken
              and j.status = uz.melisa.enums.MemoryProcessingJobStatus.IN_PROGRESS
            """)
    int renewLease(@Param("id") Long id,
                   @Param("leaseToken") UUID leaseToken,
                   @Param("lockedUntil") Timestamp lockedUntil);

    /** Abort every unfinished (non-terminal) job for a customer and release its lease. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerMemoryProcessingJob j
            set j.status = uz.melisa.enums.MemoryProcessingJobStatus.ABORTED, j.lockedUntil = null, j.leaseToken = null
            where j.customerId = :customerId
              and j.status in (uz.melisa.enums.MemoryProcessingJobStatus.PENDING,
                               uz.melisa.enums.MemoryProcessingJobStatus.IN_PROGRESS,
                               uz.melisa.enums.MemoryProcessingJobStatus.FAILED)
            """)
    int abortOpenByCustomerId(@Param("customerId") Long customerId);

    /** Delete completed jobs older than the retention cutoff. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from CustomerMemoryProcessingJob j
            where j.status = uz.melisa.enums.MemoryProcessingJobStatus.DONE
              and j.updatedAt < :cutoff
            """)
    int deleteDoneBefore(@Param("cutoff") Timestamp cutoff);
}
