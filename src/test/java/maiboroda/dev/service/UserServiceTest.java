package maiboroda.dev.service;

import maiboroda.dev.model.User;
import maiboroda.dev.storage.JsonChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    @TempDir
    Path dir;

    ServiceTestSupport t;

    @BeforeEach
    void setUp() {
        t = new ServiceTestSupport(dir);
    }

    @Test
    void registersUserWithHashedPassword() {
        User user = t.users.register("Artur", "secret-password");

        assertThat(user.username()).isEqualTo("artur");
        assertThat(user.passwordHash()).isNotEqualTo("secret-password").startsWith("$2");
        assertThat(new JsonChatRepository(dir.resolve("db.json")).findUserByUsername("artur")).contains(user);
    }

    @Test
    void rejectsDuplicateUsernameIgnoringCase() {
        t.users.register("artur", "secret-password");

        assertThatThrownBy(() -> t.users.register("ARTUR", "another-password"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void authenticatesWithCorrectPasswordOnly() {
        User user = t.users.register("artur", "secret-password");

        assertThat(t.users.authenticate(" Artur ", "secret-password")).isEqualTo(user);
        assertThatThrownBy(() -> t.users.authenticate("artur", "wrong-password"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid username or password");
        assertThatThrownBy(() -> t.users.authenticate("nobody", "secret-password"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid username or password");
    }
}
