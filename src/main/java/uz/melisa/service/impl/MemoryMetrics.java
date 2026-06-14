package uz.melisa.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer instrumentation for the customer-memory subsystem. Metric names follow the agreed
 * {@code memory.*} convention; no personal values are ever attached as tags.
 */
@Component
public class MemoryMetrics {

    private final Counter jobsCreated;
    private final Counter jobsFailed;
    private final Counter jobsDeadLetter;
    private final Counter leaseRecovered;
    private final Counter aiInvalidResponse;
    private final Counter factsCreated;
    private final Counter factsPromoted;
    private final Counter factsDeleted;
    private final Counter cacheHit;
    private final Counter cacheMiss;
    private final Counter deletionOutboxFailed;
    private final Counter deletionOutboxNeedsAttention;
    private final Timer contextLatency;

    public MemoryMetrics(MeterRegistry registry) {
        this.jobsCreated = registry.counter("memory.processing.jobs.created");
        this.jobsFailed = registry.counter("memory.processing.jobs.failed");
        this.jobsDeadLetter = registry.counter("memory.processing.jobs.dead_letter");
        this.leaseRecovered = registry.counter("memory.processing.lease.recovered");
        this.aiInvalidResponse = registry.counter("memory.ai.invalid_response");
        this.factsCreated = registry.counter("memory.facts.created");
        this.factsPromoted = registry.counter("memory.facts.promoted");
        this.factsDeleted = registry.counter("memory.facts.deleted");
        this.cacheHit = registry.counter("memory.cache.hit");
        this.cacheMiss = registry.counter("memory.cache.miss");
        this.deletionOutboxFailed = registry.counter("memory.deletion_outbox.failed");
        this.deletionOutboxNeedsAttention = registry.counter("memory.deletion_outbox.needs_attention");
        this.contextLatency = registry.timer("memory.context.latency");
    }

    public void jobCreated() {
        jobsCreated.increment();
    }

    public void jobFailed() {
        jobsFailed.increment();
    }

    public void jobDeadLetter() {
        jobsDeadLetter.increment();
    }

    public void leaseRecovered(long count) {
        if (count > 0) {
            leaseRecovered.increment(count);
        }
    }

    public void aiInvalidResponse() {
        aiInvalidResponse.increment();
    }

    public void factCreated() {
        factsCreated.increment();
    }

    public void factPromoted() {
        factsPromoted.increment();
    }

    public void factsDeleted(long count) {
        if (count > 0) {
            factsDeleted.increment(count);
        }
    }

    public void cacheHit() {
        cacheHit.increment();
    }

    public void cacheMiss() {
        cacheMiss.increment();
    }

    public void deletionOutboxFailed() {
        deletionOutboxFailed.increment();
    }

    public void deletionOutboxNeedsAttention() {
        deletionOutboxNeedsAttention.increment();
    }

    public Timer contextLatency() {
        return contextLatency;
    }
}
