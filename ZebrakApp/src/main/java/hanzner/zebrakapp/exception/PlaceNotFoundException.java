package hanzner.zebrakapp.exception;

public class PlaceNotFoundException extends BaseException {
    public PlaceNotFoundException() {
        super(ErrorCode.PLACE_NOT_FOUND);
    }

    public PlaceNotFoundException(String message) {
        super(ErrorCode.PLACE_NOT_FOUND, message);
    }

    public PlaceNotFoundException(Long id) {
        super(ErrorCode.PLACE_NOT_FOUND, "Místo nebylo nalezeno: " + id);
    }
}
