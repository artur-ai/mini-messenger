package maiboroda.dev.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Conversation(
        UUID id,
        ConversationType type,
        String name,
        List<UUID> participantIds,
        Instant createdAt
) {
    public Conversation {
        participantIds = List.copyOf(participantIds);
    }

    public boolean hasParticipant(UUID userId) {
        return participantIds.contains(userId);
    }

    public Conversation withParticipant(UUID userId) {
        if (hasParticipant(userId)) {
            return this;
        }
        var updated = new java.util.ArrayList<>(participantIds);
        updated.add(userId);
        return new Conversation(id, type, name, updated, createdAt);
    }
}
