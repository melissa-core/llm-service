package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemoryAudit;

import java.util.List;

public interface CustomerMemoryAuditRepository extends JpaRepository<CustomerMemoryAudit, Long> {

    List<CustomerMemoryAudit> findAllByCustomerId(Long customerId);

    /** Null out the fact reference in existing audit rows so a fact hard-delete is never blocked by the FK. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CustomerMemoryAudit a set a.factId = null where a.factId = :factId")
    void detachFromFact(@Param("factId") Long factId);

    /** Null fact references for all of a customer's audit rows so a full hard-delete is never FK-blocked. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CustomerMemoryAudit a set a.factId = null where a.customerId = :customerId and a.factId is not null")
    void detachAllByCustomerId(@Param("customerId") Long customerId);
}
