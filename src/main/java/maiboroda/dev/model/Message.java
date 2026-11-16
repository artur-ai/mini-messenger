package maiboroda.dev.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

public record Message(
        UUID id,
        UUID conversationId,
        UUID senderId,
        long seq,
        String text,
        Instant createdAt
) {
    public static final Comparator<Message> CHRONOLOGICAL =
            Comparator.comparing(Message::createdAt).thenComparingLong(Message::seq);
}
