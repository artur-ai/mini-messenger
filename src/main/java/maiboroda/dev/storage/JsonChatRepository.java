package maiboroda.dev.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.Message;
import maiboroda.dev.model.User;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JsonChatRepository implements ChatRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonChatRepository.class);

    private final Path file;
    private final Path tmpFile;
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final Map<UUID, User> users = new LinkedHashMap<>();
    private final Map<UUID, Conversation> conversations = new LinkedHashMap<>();
    private final Map<UUID, Message> messages = new LinkedHashMap<>();

    public JsonChatRepository(@Value("${messenger.data-file}") Path file) {
        this.file = file.toAbsolutePath();
        this.tmpFile = this.file.resolveSibling(this.file.getFileName() + ".tmp");
        load();
    }

    @Override
    public synchronized List<User> findAllUsers() {
        return List.copyOf(users.values());
    }

    @Override
    public synchronized Optional<User> findUserById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public synchronized Optional<User> findUserByUsername(String username) {
        return users.values().stream()
                .filter(u -> u.username().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public synchronized void saveUser(User user) {
        User previous = users.put(user.id(), user);
        persistOrRollback(() -> restore(users, user.id(), previous));
    }


    @Override
    public synchronized List<Conversation> findAllConversations() {
        return List.copyOf(conversations.values());
    }

    @Override
    public synchronized Optional<Conversation> findConversationById(UUID id) {
        return Optional.ofNullable(conversations.get(id));
    }

    @Override
    public synchronized void saveConversation(Conversation conversation) {
        Conversation previous = conversations.put(conversation.id(), conversation);
        persistOrRollback(() -> restore(conversations, conversation.id(), previous));
    }


    @Override
    public synchronized List<Message> findMessagesByConversation(UUID conversationId) {
        return messages.values().stream()
                .filter(m -> m.conversationId().equals(conversationId))
                .sorted(Message.CHRONOLOGICAL)
                .toList();
    }

    @Override
    public synchronized long lastSeq(UUID conversationId) {
        return messages.values().stream()
                .filter(m -> m.conversationId().equals(conversationId))
                .mapToLong(Message::seq)
                .max()
                .orElse(0);
    }

    @Override
    public synchronized void saveMessage(Message message) {
        Message previous = messages.put(message.id(), message);
        persistOrRollback(() -> restore(messages, message.id(), previous));
    }

    private void load() {
        if (Files.notExists(file)) {
            log.info("No data file at {}, creating an empty database", file);
            persist();
            return;
        }
        DatabaseSnapshot snapshot;
        try {
            snapshot = mapper.readValue(Files.readAllBytes(file), DatabaseSnapshot.class);
        } catch (JacksonException e) {
            Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + Instant.now().toEpochMilli());
            log.error("Data file {} is not valid JSON ({}). Moving it to {} and starting empty.",
                    file, e.getOriginalMessage(), backup);
            moveAside(backup);
            persist();
            return;
        } catch (IOException e) {
            throw new StorageException("Cannot read data file " + file, e);
        }
        snapshot.users().forEach(u -> users.put(u.id(), u));
        snapshot.conversations().forEach(c -> conversations.put(c.id(), c));
        snapshot.messages().forEach(m -> messages.put(m.id(), m));
        log.info("Loaded {} users, {} conversations, {} messages from {}",
                users.size(), conversations.size(), messages.size(), file);
    }

    private void persistOrRollback(Runnable rollback) {
        try {
            persist();
        } catch (StorageException e) {
            rollback.run();
            throw e;
        }
    }

    private void persist() {
        var snapshot = new DatabaseSnapshot(
                DatabaseSnapshot.CURRENT_VERSION,
                new ArrayList<>(users.values()),
                new ArrayList<>(conversations.values()),
                new ArrayList<>(messages.values()));
        try {
            Path dir = file.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            byte[] json = mapper.writeValueAsBytes(snapshot);
            try (FileChannel ch = FileChannel.open(tmpFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buf = ByteBuffer.wrap(json);
                while (buf.hasRemaining()) {
                    ch.write(buf);
                }
                ch.force(true);
            }
            try {
                Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | JacksonException e) {
            throw new StorageException("Cannot write data file " + file, e);
        }
    }

    private void moveAside(Path backup) {
        try {
            Files.move(file, backup);
        } catch (IOException e) {
            throw new StorageException("Data file " + file + " is corrupt and could not be moved aside", e);
        }
    }

    private static <V> void restore(Map<UUID, V> map, UUID id, V previous) {
        if (previous == null) {
            map.remove(id);
        } else {
            map.put(id, previous);
        }
    }
}
