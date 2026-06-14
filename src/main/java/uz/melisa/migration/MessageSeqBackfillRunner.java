package uz.melisa.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * One-time, idempotent startup migration for the per-chat {@code message_seq} column.
 *
 * <p>The column is declared nullable on the entity so Hibernate ddl-auto can add it to an existing
 * populated table. This runner then, inside a single transaction guarded by a Postgres advisory lock
 * (so concurrently starting instances serialize instead of fighting over the table):
 * <ol>
 *   <li>backfills null sequences per chat ordered by {@code created_at ASC, id ASC}, and</li>
 *   <li>tightens the column to NOT NULL once no nulls remain.</li>
 * </ol>
 * Every step is guarded so later boots are a cheap no-op. The migration is fail-fast: if it cannot
 * complete, application startup fails rather than serving traffic with unsequenced messages (which
 * would let newly created messages reuse sequence values once a later backfill runs).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSeqBackfillRunner implements ApplicationRunner {

    /** Arbitrary, stable key identifying this migration's advisory lock. */
    private static final long MIGRATION_ADVISORY_LOCK_KEY = 8714562390011L;

    private static final String ACQUIRE_ADVISORY_LOCK =
            "select pg_advisory_xact_lock(" + MIGRATION_ADVISORY_LOCK_KEY + ")";

    private static final String COUNT_MISSING_SEQ =
            "select count(*) from message where message_seq is null";

    private static final String BACKFILL_MESSAGE_SEQ = """
            update message t
            set message_seq = ranked.seq
            from (
                select id,
                       row_number() over (partition by chat_id order by created_at asc, id asc) as seq
                from message
            ) ranked
            where t.id = ranked.id
              and t.message_seq is null
            """;

    private static final String IS_MESSAGE_SEQ_NULLABLE = """
            select is_nullable from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'message'
              and column_name = 'message_seq'
            """;

    private static final String SET_MESSAGE_SEQ_NOT_NULL =
            "alter table message alter column message_seq set not null";

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(ApplicationArguments args) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> migrate());
        log.info("message_seq migration completed");
    }

    private void migrate() {
        jdbcTemplate.execute(ACQUIRE_ADVISORY_LOCK);
        backfillMissingSequences();
        enforceNotNull();
    }

    private void backfillMissingSequences() {
        long missing = countMissingSequences();
        if (missing == 0) {
            log.info("message_seq backfill skipped; every message already has a sequence");
            return;
        }
        int updated = jdbcTemplate.update(BACKFILL_MESSAGE_SEQ);
        log.info("message_seq backfill assigned a sequence to {} message rows", updated);
    }

    private void enforceNotNull() {
        String nullable = jdbcTemplate.queryForObject(IS_MESSAGE_SEQ_NULLABLE, String.class);
        if (!"YES".equalsIgnoreCase(nullable)) {
            return;
        }
        jdbcTemplate.execute(SET_MESSAGE_SEQ_NOT_NULL);
        log.info("message_seq column tightened to NOT NULL");
    }

    private long countMissingSequences() {
        Long missing = jdbcTemplate.queryForObject(COUNT_MISSING_SEQ, Long.class);
        return missing == null ? 0L : missing;
    }
}
