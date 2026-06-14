package uz.melisa.projection;

/**
 * Per-chat high-water mark of message_seq, used by the memory module's
 * deletion/consent flows to learn the latest summarizable position of each chat.
 */
public interface ChatMessageSeqWatermark {

    Long getChatId();

    Long getWatermark();
}
