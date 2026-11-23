package maiboroda.dev.service;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.ConversationType;
import maiboroda.dev.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationServiceTest {

    @TempDir
    Path dir;

    ServiceTestSupport t;
    User artur;
    User olena;
    User taras;

    @BeforeEach
    void setUp() {
        t = new ServiceTestSupport(dir);
        artur = t.users.register("artur", "password-1");
        olena = t.users.register("olena", "password-2");
        taras = t.users.register("taras", "password-3");
    }

    @Test
    void createsGroupChannelWithThreeMembers() {
        Conversation channel = t.conversations.createChannel(artur.id(), "Floor-3", List.of("olena", "taras"));

        assertThat(channel.name()).isEqualTo("#floor-3");
        assertThat(channel.participantIds()).containsExactly(artur.id(), olena.id(), taras.id());
        assertThatThrownBy(() -> t.conversations.createChannel(olena.id(), "#floor-3", List.of()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void joinsDefaultChannel() {
        t.conversations.ensureChannelsExist(List.of("#kitchen"));

        Conversation kitchen = t.conversations.join(olena.id(), "kitchen");

        assertThat(kitchen.hasParticipant(olena.id())).isTrue();
        assertThat(t.conversations.listFor(olena.id())).extracting(Conversation::name).containsExactly("#kitchen");
    }

    @Test
    void reusesExistingDirectConversation() {
        Conversation first = t.conversations.openDirect(artur.id(), "olena");
        Conversation second = t.conversations.openDirect(olena.id(), "artur");

        assertThat(first.type()).isEqualTo(ConversationType.DIRECT);
        assertThat(second.id()).isEqualTo(first.id());
        assertThatThrownBy(() -> t.conversations.openDirect(artur.id(), "artur"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void outsidersCannotAccessConversation() {
        Conversation direct = t.conversations.openDirect(artur.id(), "olena");

        assertThatThrownBy(() -> t.conversations.requireParticipant(direct.id(), taras.id()))
                .isInstanceOf(ForbiddenException.class);
    }
}
