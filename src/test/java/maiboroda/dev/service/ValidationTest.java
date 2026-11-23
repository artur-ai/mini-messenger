package maiboroda.dev.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\n", "\t"})
    void rejectsEmptyMessages(String text) {
        assertThatThrownBy(() -> Validation.messageText(text))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Message cannot be empty");
    }

    @Test
    void rejectsNullMessage() {
        assertThatThrownBy(() -> Validation.messageText(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void acceptsMessageAtExactLimitAndRejectsOneMore() {
        assertThat(Validation.messageText("a".repeat(512))).hasSize(512);
        assertThatThrownBy(() -> Validation.messageText("a".repeat(513)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void countsEmojiAsOneCharacter() {
        assertThat(Validation.messageText("🍕".repeat(512))).isNotEmpty();
    }

    @Test
    void trimsAndNormalizesLineEndings() {
        assertThat(Validation.messageText("  hi\r\nthere  ")).isEqualTo("hi\nthere");
    }

    @Test
    void rejectsControlAndBidiCharacters() {
        assertThatThrownBy(() -> Validation.messageText("hi\u0000")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Validation.messageText("abc‮def")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsTooManyLines() {
        assertThatThrownBy(() -> Validation.messageText("a\n".repeat(11) + "a"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void normalizesUsernamesAndChannels() {
        assertThat(Validation.username("  Artur_3 ")).isEqualTo("artur_3");
        assertThat(Validation.channelName("Kitchen")).isEqualTo("#kitchen");
        assertThat(Validation.channelName("#floor-3")).isEqualTo("#floor-3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "with space", "кирило", "abcdefghijklmnopqrstuv"})
    void rejectsBadUsernames(String username) {
        assertThatThrownBy(() -> Validation.username(username)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsShortPasswords() {
        assertThatThrownBy(() -> Validation.password("1234567")).isInstanceOf(ValidationException.class);
    }
}
