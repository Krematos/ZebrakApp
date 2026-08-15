package hanzner.zebrakapp.exception;

public class ImageNotFoundException extends BaseException {
    public ImageNotFoundException() {
        super(ErrorCode.IMAGE_NOT_FOUND);
    }

    public ImageNotFoundException(String message) {
        super(ErrorCode.IMAGE_NOT_FOUND, message);
    }

    public ImageNotFoundException(Long imageId) {
        super(ErrorCode.IMAGE_NOT_FOUND, "Obrázek nebyl nalezen: " + imageId);
    }
}
