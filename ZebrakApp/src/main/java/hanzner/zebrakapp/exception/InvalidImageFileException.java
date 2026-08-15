package hanzner.zebrakapp.exception;

public class InvalidImageFileException extends BaseException {
    public InvalidImageFileException() {
        super(ErrorCode.INVALID_IMAGE_FILE);
    }

    public InvalidImageFileException(String message) {
        super(ErrorCode.INVALID_IMAGE_FILE, message);
    }
}
