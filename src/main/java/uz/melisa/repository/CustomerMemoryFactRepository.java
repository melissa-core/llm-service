package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemoryFact;
import uz.melisa.enums.MemoryFactStatus;
import uz.melisa.enums.MemoryFactType;

import java.util.List;
import java.util.Optional;

public interface CustomerMemoryFactRepository extends JpaRepository<CustomerMemoryFact, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CustomerMemoryFact f where f.customerId = :customerId")
    int deleteByCustomerId(@Param("customerId") Long customerId);

    List<CustomerMemoryFact> findAllByCustomerIdAndStatus(Long customerId, MemoryFactStatus status);

    @Query("""
            select f from CustomerMemoryFact f
            where f.customerId = :customerId
              and f.factType = :factType
              and f.factKey = :factKey
              and f.normalizedValue = :normalizedValue
              and f.status = :status
            """)
    Optional<CustomerMemoryFact> findMatchingFact(@Param("customerId") Long customerId,
                                                  @Param("factType") MemoryFactType factType,
                                                  @Param("factKey") String factKey,
                                                  @Param("normalizedValue") String normalizedValue,
                                                  @Param("status") MemoryFactStatus status);
}
