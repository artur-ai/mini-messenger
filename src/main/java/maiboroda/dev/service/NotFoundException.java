package maiboroda.dev.service;

public final class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
