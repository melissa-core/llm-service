package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uz.melisa.domain.EmbeddingDetails;

public interface EmbeddingDetailsRepository extends JpaRepository<EmbeddingDetails, Long>, JpaSpecificationExecutor<EmbeddingDetails> {
}
