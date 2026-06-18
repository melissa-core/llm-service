package uz.melisa.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageCode {

    CHAT_SAVED("chat.saved"),
    CHAT_UPDATED("chat.updated"),
    CHAT_DELETED("chat.deleted"),
    CHAT_NOT_FOUND("chat.not_found"),
    CHAT_TEMPORARY_NOT_FOUND("chat.temporary_not_found"),
    CHAT_ACTIVATED("chat.activated"),

    MESSAGE_DEVICE_ID_REQUIRED("message.device_id_required"),
    MESSAGE_EMPTY("message.empty"),
    MESSAGE_DELETED("message.deleted"),
    MESSAGE_NOT_FOUND("message.not_found"),

    AUTH_USER_DETAILS_INVALID("auth.user_details_invalid"),
    AUTH_INVALID_CREDENTIALS("auth.invalid_credentials"),
    AUTH_USER_NOT_FOUND("auth.user_not_found"),
    AUTH_SESSION_INVALID("auth.session_invalid"),

    COMMON_SOMETHING_WENT_WRONG("common.something_went_wrong"),
    COMMON_EMPTY_MESSAGES("common.empty_messages"),
    COMMON_INVALID_INPUT("common.invalid_input"),
    COMMON_DATA_NOT_FOUND("common.data_not_found"),
    COMMON_PAGE_NOT_FOUND("common.page_not_found");

    private final String key;
}
