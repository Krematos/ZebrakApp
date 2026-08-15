package hanzner.zebrakapp.exception;

public class FileStorageException extends BaseException {
    public FileStorageException() {
        super(ErrorCode.FILE_STORAGE_ERROR);
    }

    public FileStorageException(String message) {
        super(ErrorCode.FILE_STORAGE_ERROR, message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(ErrorCode.FILE_STORAGE_ERROR, message, cause);
    }
}
