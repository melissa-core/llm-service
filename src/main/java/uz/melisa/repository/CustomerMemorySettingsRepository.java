package uz.melisa.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.melisa.domain.CustomerMemorySettings;

import java.util.Optional;

public interface CustomerMemorySettingsRepository extends JpaRepository<CustomerMemorySettings, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CustomerMemorySettings s where s.customerId = :customerId")
    Optional<CustomerMemorySettings> findByCustomerIdForUpdate(@Param("customerId") Long customerId);
}
