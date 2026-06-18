package uz.melisa.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.melisa.enums.MessageCode;
import uz.melisa.exp.ItemNotFoundException;
import uz.melisa.repository.ChatRepository;
import uz.melisa.repository.MessageRepository;

/**
 * Assigns the next per-chat monotonic message_seq.
 *
 * <p>Must run inside the caller's transaction ({@code Propagation.MANDATORY}). It takes a
 * pessimistic write lock on the chat row first, so concurrent message creation for the same
 * chat is serialized and {@code max(message_seq) + 1} can never hand out a duplicate.
 */
@Service
@RequiredArgsConstructor
public class MessageSequenceService {

    private static final long FIRST_MESSAGE_SEQ = 1L;

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public long nextMessageSeq(Long chatId) {
        chatRepository.findByIdForUpdate(chatId)
                .orElseThrow(() -> new ItemNotFoundException(MessageCode.CHAT_NOT_FOUND));
        long currentMax = messageRepository.findMaxMessageSeqByChatId(chatId);
        return currentMax + FIRST_MESSAGE_SEQ;
    }
}
