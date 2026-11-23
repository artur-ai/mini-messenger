package maiboroda.dev.web;

import maiboroda.dev.service.AuthenticationException;
import maiboroda.dev.service.ConflictException;
import maiboroda.dev.service.DomainException;
import maiboroda.dev.service.ForbiddenException;
import maiboroda.dev.service.NotFoundException;
import maiboroda.dev.service.ValidationException;
import maiboroda.dev.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(int status, String error, String message, Instant timestamp) {
    }

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> domain(DomainException e) {
        HttpStatus status = switch (e) {
            case ValidationException v -> HttpStatus.BAD_REQUEST;
            case AuthenticationException a -> HttpStatus.UNAUTHORIZED;
            case ForbiddenException f -> HttpStatus.FORBIDDEN;
            case NotFoundException n -> HttpStatus.NOT_FOUND;
            case ConflictException c -> HttpStatus.CONFLICT;
        };
        return error(status, e.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiError> storage(StorageException e) {
        log.error("Storage failure", e);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Storage is temporarily unavailable, nothing was saved");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Malformed JSON request");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for '" + e.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "Missing parameter '" + e.getParameterName() + "'");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message, Instant.now()));
    }
}
