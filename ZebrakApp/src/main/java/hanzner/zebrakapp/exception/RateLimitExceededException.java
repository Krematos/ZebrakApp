package hanzner.zebrakapp.exception;

public class RateLimitExceededException extends BaseException {

    public RateLimitExceededException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    public RateLimitExceededException(String message) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message);
    }
}
