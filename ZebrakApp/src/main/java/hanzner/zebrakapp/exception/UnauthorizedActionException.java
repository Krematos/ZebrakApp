package hanzner.zebrakapp.exception;

public class UnauthorizedActionException extends BaseException {
    public UnauthorizedActionException() {
        super(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    public UnauthorizedActionException(String message) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, message);
    }
}
