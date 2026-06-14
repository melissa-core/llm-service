package uz.melisa.dto.memory;

import java.util.List;

/**
 * Assembled L1 memory context for a customer, cached in Redis and rebuilt from Postgres on miss.
 * Carries the generation and factVersion it was built at so a stale entry can never be mistaken for
 * a current one. When memory is disabled the context is returned with an empty fact list.
 */
public record MemoryContext(
        Long customerId,
        long generation,
        long factVersion,
        List<MemoryFactView> facts,
        List<String> summaries
) {
}
