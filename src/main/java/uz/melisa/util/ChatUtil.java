package uz.melisa.util;

import org.apache.commons.lang.WordUtils;
import uz.melisa.domain.Chat;

public final class ChatUtil {

    private ChatUtil() {
    }

    public static Chat buildChat(Long userId, String chatTitle) {
        Chat chat = new Chat();
        chat.setUserId(userId);
        chat.setTitle(chatTitle);
        return chat;
    }

    public static Chat buildChatByDeviceId(String deviceId, String message) {
        String chatTitle = getChatTitle(message);
        Chat chat = new Chat();
        chat.setDeviceId(deviceId);
        chat.setTitle(chatTitle);
        chat.setTemporary(true);
        return chat;
    }

    public static String getChatTitle(String message) {
        String title = message.length() > 11
                ? message.substring(0, 11) + "..."
                : message;

        return WordUtils.capitalizeFully(title);
    }
}
