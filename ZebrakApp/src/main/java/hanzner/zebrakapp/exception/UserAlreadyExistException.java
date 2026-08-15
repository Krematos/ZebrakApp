package hanzner.zebrakapp.exception;

public class UserAlreadyExistException extends BaseException {
    public UserAlreadyExistException() {
        super(ErrorCode.USER_ALREADY_EXISTS);
    }

    public UserAlreadyExistException(String message) {
        super(ErrorCode.USER_ALREADY_EXISTS, message);
    }
}
