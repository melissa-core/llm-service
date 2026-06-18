package uz.melisa.exp;

import lombok.Getter;
import uz.melisa.enums.ApiResponseStatus;
import uz.melisa.enums.MessageCode;

@Getter
public class BusinessException extends RuntimeException {

    private final transient MessageCode messageCode;
    private final ApiResponseStatus status;

    public BusinessException(MessageCode messageCode, ApiResponseStatus status) {
        super(messageCode.getKey());
        this.messageCode = messageCode;
        this.status = status;
    }

    public BusinessException(String message, ApiResponseStatus status) {
        super(message);
        this.messageCode = null;
        this.status = status;
    }
}
