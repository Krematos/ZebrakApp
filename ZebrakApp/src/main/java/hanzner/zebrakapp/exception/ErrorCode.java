package hanzner.zebrakapp.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    USER_ALREADY_EXISTS("Uživatel s tímto e-mailem již existuje", HttpStatus.CONFLICT),
    USER_NOT_FOUND("Uživatel nebyl nalezen", HttpStatus.NOT_FOUND),
    PLACE_NOT_FOUND("Místo nebylo nalezeno", HttpStatus.NOT_FOUND),
    PLACE_NOT_APPROVED("Místo zatím nebylo schváleno", HttpStatus.BAD_REQUEST),
    IMAGE_NOT_FOUND("Obrázek nebyl nalezen", HttpStatus.NOT_FOUND),
    IMAGE_DOES_NOT_BELONG_TO_PLACE("Obrázek nepatří k tomuto místu", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_FILE("Nepodporovaný formát nebo prázdný soubor obrázku", HttpStatus.BAD_REQUEST),
    FILE_STORAGE_ERROR("Chyba při práci se souborovým úložištěm", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED_ACCESS("Nemáte oprávnění k provedení této operace", HttpStatus.FORBIDDEN),
    RATE_LIMIT_EXCEEDED("Příliš mnoho požadavků. Počkejte prosím chvíli před vytvořením dalšího místa.", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_PASSWORD("Zadané heslo není správné", HttpStatus.BAD_REQUEST),
    PAYLOAD_TOO_LARGE("Velikost nahraného souboru překročila maximální povolený limit", HttpStatus.PAYLOAD_TOO_LARGE),
    VALIDATION_ERROR("Chyba validace vstupních dat", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("Došlo k neočekávané chybě serveru", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String defaultMessage, HttpStatus httpStatus) {
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
