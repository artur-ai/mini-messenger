package maiboroda.dev.service;

public abstract sealed class DomainException extends RuntimeException
        permits ValidationException, NotFoundException, ForbiddenException, ConflictException, AuthenticationException {

    protected DomainException(String message) {
        super(message);
    }
}
