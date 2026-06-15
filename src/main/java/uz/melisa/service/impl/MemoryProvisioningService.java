package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.domain.CustomerMemoryChatState;
import uz.melisa.domain.CustomerMemorySettings;
import uz.melisa.repository.CustomerMemoryChatStateRepository;
import uz.melisa.repository.CustomerMemorySettingsRepository;

import java.sql.Timestamp;

/**
 * Ensures customer memory is provisioned for every authenticated customer. Memory is ON by default
 * for all users (product decision 2026-06-15): a settings row (memory_enabled=true) is created on
 * first contact, and a chat_state row is created per chat so the segment scheduler discovers and
 * processes it. A customer who has explicitly opted out (opted_out_at set) is never silently
 * re-enabled. Provisioning is idempotent; after the first message per customer/chat the existence
 * checks short-circuit.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryProvisioningService {

    private final CustomerMemorySettingsRepository settingsRepository;
    private final CustomerMemoryChatStateRepository chatStateRepository;

    @Transactional
    public void ensureProvisioned(Long customerId, Long chatId) {
        if (customerId == null) {
            return;   // guests have no customer memory
        }
        ensureMemoryEnabled(customerId);
        ensureChatTracked(customerId, chatId);
    }

    private void ensureMemoryEnabled(Long customerId) {
        CustomerMemorySettings settings = settingsRepository.findById(customerId).orElse(null);
        if (settings == null) {
            settings = CustomerMemorySettings.builder().customerId(customerId).build();
            settings.setMemoryEnabled(true);
            settings.setConsentAt(now());
            settingsRepository.save(settings);
            log.info("memory auto-enabled (default-on) customerId={}", customerId);
            return;
        }
        if (!settings.isMemoryEnabled() && settings.getOptedOutAt() == null) {
            settings.setMemoryEnabled(true);
            settings.setConsentAt(now());
            settingsRepository.save(settings);
            log.info("memory enabled for existing settings customerId={}", customerId);
        }
    }

    private void ensureChatTracked(Long customerId, Long chatId) {
        if (chatId == null || chatStateRepository.existsById(chatId)) {
            return;
        }
        chatStateRepository.save(CustomerMemoryChatState.builder()
                .chatId(chatId)
                .customerId(customerId)
                .lastCompletedMessageSeq(0L)   // process the whole chat, incl. the triggering message
                .build());
        log.info("memory chat tracking started customerId={} chatId={}", customerId, chatId);
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
