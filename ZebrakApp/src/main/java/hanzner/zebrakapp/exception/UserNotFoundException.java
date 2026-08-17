package hanzner.zebrakapp.exception;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }

    public UserNotFoundException(Long id) {
        super(ErrorCode.USER_NOT_FOUND, "Uživatel s ID " + id + " nebyl nalezen.");
    }
}
