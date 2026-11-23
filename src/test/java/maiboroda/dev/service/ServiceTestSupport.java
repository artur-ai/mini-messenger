package maiboroda.dev.service;

import maiboroda.dev.storage.JsonChatRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

final class ServiceTestSupport {

    final Clock clock = Clock.fixed(Instant.parse("2026-11-19T09:00:00Z"), ZoneOffset.UTC);
    final JsonChatRepository repository;
    final UserService users;
    final ConversationService conversations;
    final MessageService messages;

    ServiceTestSupport(Path dir) {
        repository = new JsonChatRepository(dir.resolve("db.json"));
        users = new UserService(repository, new BCryptPasswordEncoder(4), clock);
        conversations = new ConversationService(repository, users, clock);
        messages = new MessageService(repository, conversations, clock);
    }
}
