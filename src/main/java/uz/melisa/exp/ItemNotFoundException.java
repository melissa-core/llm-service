package uz.melisa.exp;

import uz.melisa.enums.ApiResponseStatus;
import uz.melisa.enums.MessageCode;

public class ItemNotFoundException extends BusinessException {

    public ItemNotFoundException(MessageCode messageCode) {
        super(messageCode, ApiResponseStatus.NOT_FOUND);
    }

    public ItemNotFoundException(String message) {
        super(message, ApiResponseStatus.NOT_FOUND);
    }
}
