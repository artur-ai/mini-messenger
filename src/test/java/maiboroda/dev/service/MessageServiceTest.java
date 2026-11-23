package maiboroda.dev.service;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.Message;
import maiboroda.dev.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageServiceTest {

    @TempDir
    Path dir;

    ServiceTestSupport t;
    User artur;
    User olena;
    User taras;
    Conversation kitchen;

    @BeforeEach
    void setUp() {
        t = new ServiceTestSupport(dir);
        artur = t.users.register("artur", "password-1");
        olena = t.users.register("olena", "password-2");
        taras = t.users.register("taras", "password-3");
        kitchen = t.conversations.createChannel(artur.id(), "#kitchen", List.of("olena"));
    }

    @Test
    void sendsMessagesWithIncreasingSeqInSameMillisecond() {
        Message first = t.messages.send(artur.id(), kitchen.id(), "who took my pot?");
        Message second = t.messages.send(olena.id(), kitchen.id(), "  it was taras  ");

        assertThat(first.seq()).isEqualTo(1);
        assertThat(second.seq()).isEqualTo(2);
        assertThat(second.text()).isEqualTo("it was taras");
        assertThat(first.createdAt()).isEqualTo(second.createdAt());
        assertThat(t.messages.history(artur.id(), kitchen.id(), 0, 100)).containsExactly(first, second);
    }

    @Test
    void rejectsEmptyMessageAndSavesNothing() {
        assertThatThrownBy(() -> t.messages.send(artur.id(), kitchen.id(), "   "))
                .isInstanceOf(ValidationException.class);

        assertThat(t.messages.history(artur.id(), kitchen.id(), 0, 100)).isEmpty();
    }

    @Test
    void nonMemberCannotSendOrRead() {
        assertThatThrownBy(() -> t.messages.send(taras.id(), kitchen.id(), "hi"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> t.messages.history(taras.id(), kitchen.id(), 0, 100))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void historyAfterSeqReturnsOnlyNewMessages() {
        t.messages.send(artur.id(), kitchen.id(), "one");
        t.messages.send(artur.id(), kitchen.id(), "two");
        Message three = t.messages.send(artur.id(), kitchen.id(), "three");

        assertThat(t.messages.history(olena.id(), kitchen.id(), 2, 100)).containsExactly(three);
        assertThat(t.messages.history(olena.id(), kitchen.id(), 0, 1)).containsExactly(three);
    }

    @Test
    void searchFindsOnlyInOwnConversationsIgnoringCase() {
        t.messages.send(artur.id(), kitchen.id(), "Laundry room is free");
        Conversation direct = t.conversations.openDirect(taras.id(), "olena");
        t.messages.send(taras.id(), direct.id(), "laundry secret");

        assertThat(t.messages.search(artur.id(), "LAUNDRY")).extracting(Message::text)
                .containsExactly("Laundry room is free");
        assertThat(t.messages.search(olena.id(), "laundry")).hasSize(2);
        assertThatThrownBy(() -> t.messages.search(artur.id(), "a"))
                .isInstanceOf(ValidationException.class);
    }
}
