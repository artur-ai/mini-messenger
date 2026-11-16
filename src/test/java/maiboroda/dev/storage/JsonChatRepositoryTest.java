package maiboroda.dev.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.ConversationType;
import maiboroda.dev.model.Message;
import maiboroda.dev.model.User;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JsonChatRepositoryTest {

    @TempDir
    Path dir;

    @Test
    void createsEmptyDatabaseWhenFileIsMissing() {
        Path db = dir.resolve("data/db.json");

        var repo = new JsonChatRepository(db);

        assertThat(db).exists();
        assertThat(repo.findAllUsers()).isEmpty();
    }

    @Test
    void dataSurvivesRestart() {
        Path db = dir.resolve("db.json");
        var repo = new JsonChatRepository(db);

        var alice = new User(UUID.randomUUID(), "alice", "hash", Instant.now());
        var bob = new User(UUID.randomUUID(), "bob", "hash", Instant.now());
        repo.saveUser(alice);
        repo.saveUser(bob);
        var kitchen = new Conversation(UUID.randomUUID(), ConversationType.CHANNEL, "#kitchen",
                List.of(alice.id(), bob.id()), Instant.now());
        repo.saveConversation(kitchen);
        var msg = new Message(UUID.randomUUID(), kitchen.id(), alice.id(), 1,
                "who took my pot?", Instant.parse("2026-11-16T18:00:00Z"));
        repo.saveMessage(msg);

        var reopened = new JsonChatRepository(db);

        assertThat(reopened.findUserByUsername("ALICE")).contains(alice);
        assertThat(reopened.findConversationById(kitchen.id())).contains(kitchen);
        assertThat(reopened.findMessagesByConversation(kitchen.id())).containsExactly(msg);
        assertThat(reopened.lastSeq(kitchen.id())).isEqualTo(1);
    }

    @Test
    void messagesAreOrderedByTimeThenSeqEvenInTheSameMillisecond() {
        var repo = new JsonChatRepository(dir.resolve("db.json"));
        UUID conv = UUID.randomUUID();
        UUID sender = UUID.randomUUID();
        Instant sameMoment = Instant.parse("2026-11-16T18:00:00Z");

        var third = new Message(UUID.randomUUID(), conv, sender, 3, "c", sameMoment);
        var first = new Message(UUID.randomUUID(), conv, sender, 1, "a", sameMoment.minusSeconds(5));
        var second = new Message(UUID.randomUUID(), conv, sender, 2, "b", sameMoment);
        repo.saveMessage(third);
        repo.saveMessage(first);
        repo.saveMessage(second);

        assertThat(repo.findMessagesByConversation(conv)).containsExactly(first, second, third);
    }

    @Test
    void corruptFileIsMovedAsideAndAppStartsEmpty() throws Exception {
        Path db = dir.resolve("db.json");
        Files.writeString(db, "{ this is not json");

        var repo = new JsonChatRepository(db);

        assertThat(repo.findAllUsers()).isEmpty();
        try (var files = Files.list(dir)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .anyMatch(name -> name.startsWith("db.json.corrupt-"));
        }
    }

    @Test
    void noTmpFileLeftAfterSuccessfulWrite() {
        Path db = dir.resolve("db.json");
        var repo = new JsonChatRepository(db);

        repo.saveUser(new User(UUID.randomUUID(), "carol", "hash", Instant.now()));

        assertThat(dir.resolve("db.json.tmp")).doesNotExist();
    }
}
