package uz.melisa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uz.melisa.domain.MessageMetadata;

public interface MessageMetadataRepository extends JpaRepository<MessageMetadata, Long>, JpaSpecificationExecutor<MessageMetadata> {
}
