package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uz.melisa.domain.MessageLanguageDetails;

public interface MessageLanguageDetailsRepository extends JpaRepository<MessageLanguageDetails, Long>, JpaSpecificationExecutor<MessageLanguageDetails> {
}
