package exceptions;

import enums.ErrorCode;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int httpStatus;

    public ApiException(ErrorCode errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}