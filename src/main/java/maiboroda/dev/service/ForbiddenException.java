package maiboroda.dev.service;

public final class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }
}
