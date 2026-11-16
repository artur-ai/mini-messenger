package maiboroda.dev.model;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String username,
        String passwordHash,
        Instant createdAt
) {
}
