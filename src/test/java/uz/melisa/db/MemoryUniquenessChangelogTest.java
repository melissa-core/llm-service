package uz.melisa.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryUniquenessChangelogTest {

    @Test
    void changelogDeclaresAllDurableMemoryUniquenessInvariants() throws IOException {
        String xml;
        try (var input = getClass().getResourceAsStream("/db/changelog/changes/004-memory-uniqueness.xml")) {
            if (input == null) {
                throw new AssertionError("004-memory-uniqueness.xml is missing from resources");
            }
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(xml.contains("uq_cm_fact_active_value"));
        assertTrue(xml.contains("uq_cm_fact_active_single_key"));
        assertTrue(xml.contains("uq_cm_candidate_pending_value"));
        assertTrue(xml.contains("uq_cm_candidate_evidence_chat"));
        assertTrue(xml.contains("uq_cm_episode_chat_segment"));
        assertTrue(xml.contains("uq_cm_proc_job_idempotency_key"));
        assertTrue(xml.contains("WHERE status = 'ACTIVE' AND cardinality = 'SINGLE'"));
        assertTrue(xml.contains("WHERE status = 'PENDING'"));
    }
}
