package hanzner.zebrakapp.exception;

public class InvalidPasswordException extends BaseException {
    public InvalidPasswordException() {
        super(ErrorCode.INVALID_PASSWORD);
    }

    public InvalidPasswordException(String message) {
        super(ErrorCode.INVALID_PASSWORD, message);
    }
}
