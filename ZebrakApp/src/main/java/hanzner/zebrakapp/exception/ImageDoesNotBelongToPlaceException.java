package hanzner.zebrakapp.exception;

public class ImageDoesNotBelongToPlaceException extends BaseException {
    public ImageDoesNotBelongToPlaceException() {
        super(ErrorCode.IMAGE_DOES_NOT_BELONG_TO_PLACE);
    }

    public ImageDoesNotBelongToPlaceException(String message) {
        super(ErrorCode.IMAGE_DOES_NOT_BELONG_TO_PLACE, message);
    }
}
