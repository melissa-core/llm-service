package uz.melisa.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import uz.melisa.enums.ApiResponseStatus;

@Data
@AllArgsConstructor
public class CommonResponse<T> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;
    @JsonIgnore
    private ApiResponseStatus status;

    private CommonResponse(T data) {
        this.data = data;
        this.status = ApiResponseStatus.OK;
    }

    private CommonResponse(String message, ApiResponseStatus status) {
        this.data = null;
        this.errorMessage = message;
        this.status = status;
    }

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(data);
    }

    public static <T> CommonResponse<T> failure(String message, ApiResponseStatus status) {
        return new CommonResponse<>(message, status);
    }
}