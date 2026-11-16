package maiboroda.dev.storage;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.Message;
import maiboroda.dev.model.User;

import java.util.List;

record DatabaseSnapshot(
        int schemaVersion,
        List<User> users,
        List<Conversation> conversations,
        List<Message> messages
) {
    static final int CURRENT_VERSION = 1;

    static DatabaseSnapshot empty() {
        return new DatabaseSnapshot(CURRENT_VERSION, List.of(), List.of(), List.of());
    }

    DatabaseSnapshot {
        users = users == null ? List.of() : users;
        conversations = conversations == null ? List.of() : conversations;
        messages = messages == null ? List.of() : messages;
    }
}
