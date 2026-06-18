package uz.melisa.exp;

import uz.melisa.enums.ApiResponseStatus;
import uz.melisa.enums.MessageCode;

public class BadRequestException extends BusinessException {

    public BadRequestException(MessageCode messageCode) {
        super(messageCode, ApiResponseStatus.BAD_REQUEST);
    }

    public BadRequestException(String message) {
        super(message, ApiResponseStatus.BAD_REQUEST);
    }
}
