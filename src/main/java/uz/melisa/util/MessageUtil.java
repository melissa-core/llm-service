package uz.melisa.util;

import uz.melisa.domain.Message;
import uz.melisa.enums.MessageAuthorityType;
import uz.melisa.enums.MessageModelType;
import uz.melisa.enums.MessageType;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static Message buildModelMessage(String textMessage,Boolean hasSuggestions, Long chatId, Long userId) {
        Message message = new Message();
        message.setChatId(chatId);
        message.setUserId(userId);
        message.setText(textMessage);
        message.setMessageType(MessageType.TEXT);
        message.setHasProductSuggestions(hasSuggestions);
        message.setMessageAuthorityType(MessageAuthorityType.MODEL);
        message.setMessageModelType(MessageModelType.CLAUDE);
        return message;
    }

    public static Message buildUserMessage(String messageText, Long chatId, Long userId) {
        Message message = new Message();
        message.setMessageType(MessageType.TEXT);
        message.setMessageAuthorityType(MessageAuthorityType.USER);
        message.setChatId(chatId);
        message.setUserId(userId);
        message.setText(messageText);
        return message;
    }
}
