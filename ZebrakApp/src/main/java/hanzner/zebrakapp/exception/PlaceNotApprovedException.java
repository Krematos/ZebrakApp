package hanzner.zebrakapp.exception;

public class PlaceNotApprovedException extends BaseException {
    public PlaceNotApprovedException() {
        super(ErrorCode.PLACE_NOT_APPROVED);
    }

    public PlaceNotApprovedException(String message) {
        super(ErrorCode.PLACE_NOT_APPROVED, message);
    }
}
