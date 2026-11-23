package maiboroda.dev.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Validation {

    public static final int MAX_MESSAGE_LENGTH = 512;
    public static final int MAX_MESSAGE_LINES = 10;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_BYTES = 72;
    public static final int MIN_SEARCH_LENGTH = 2;
    public static final int MAX_SEARCH_LENGTH = 100;

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9_]{3,20}");
    private static final Pattern CHANNEL = Pattern.compile("#[a-z0-9_-]{2,30}");

    private Validation() {
    }

    public static String username(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Username is required");
        }
        String username = raw.strip().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(username).matches()) {
            throw new ValidationException("Username must be 3-20 characters: a-z, 0-9, _");
        }
        return username;
    }

    public static void password(String raw) {
        if (raw == null || raw.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (raw.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ValidationException("Password is too long (max " + MAX_PASSWORD_BYTES + " bytes)");
        }
        if (raw.isBlank()) {
            throw new ValidationException("Password cannot be only spaces");
        }
    }

    public static String channelName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Channel name is required");
        }
        String name = raw.strip().toLowerCase(Locale.ROOT);
        if (!name.startsWith("#")) {
            name = "#" + name;
        }
        if (!CHANNEL.matcher(name).matches()) {
            throw new ValidationException("Channel name must be # followed by 2-30 characters: a-z, 0-9, _, -");
        }
        return name;
    }

    public static String messageText(String raw) {
        if (raw == null) {
            throw new ValidationException("Message cannot be empty");
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (text.isEmpty()) {
            throw new ValidationException("Message cannot be empty");
        }
        if (text.codePointCount(0, text.length()) > MAX_MESSAGE_LENGTH) {
            throw new ValidationException("Message is too long (max " + MAX_MESSAGE_LENGTH + " characters)");
        }
        if (text.lines().count() > MAX_MESSAGE_LINES) {
            throw new ValidationException("Message has too many lines (max " + MAX_MESSAGE_LINES + ")");
        }
        if (text.codePoints().anyMatch(Validation::isForbiddenCodePoint)) {
            throw new ValidationException("Message contains invalid characters");
        }
        return text;
    }

    public static String searchQuery(String raw) {
        String query = raw == null ? "" : raw.strip();
        if (query.length() < MIN_SEARCH_LENGTH) {
            throw new ValidationException("Search query must be at least " + MIN_SEARCH_LENGTH + " characters");
        }
        if (query.length() > MAX_SEARCH_LENGTH) {
            throw new ValidationException("Search query is too long (max " + MAX_SEARCH_LENGTH + " characters)");
        }
        return query;
    }

    private static boolean isForbiddenCodePoint(int cp) {
        if (cp == '\n' || cp == '\t') {
            return false;
        }
        if (Character.isISOControl(cp)) {
            return true;
        }
        boolean bidiOverride = (cp >= 0x202A && cp <= 0x202E) || (cp >= 0x2066 && cp <= 0x2069);
        boolean surrogate = Character.getType(cp) == Character.SURROGATE;
        return bidiOverride || surrogate;
    }
}
