package maiboroda.dev.service;

import maiboroda.dev.model.User;
import maiboroda.dev.storage.ChatRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final ChatRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String dummyHash;

    public UserService(ChatRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public synchronized User register(String rawUsername, String rawPassword) {
        String username = Validation.username(rawUsername);
        Validation.password(rawPassword);
        if (repository.findUserByUsername(username).isPresent()) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        User user = new User(UUID.randomUUID(), username, passwordEncoder.encode(rawPassword), clock.instant());
        repository.saveUser(user);
        return user;
    }

    public User authenticate(String rawUsername, String rawPassword) {
        String username = rawUsername == null ? "" : rawUsername.strip().toLowerCase(Locale.ROOT);
        String password = rawPassword == null ? "" : rawPassword;
        Optional<User> user = repository.findUserByUsername(username);
        String hash = user.map(User::passwordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !matches) {
            throw new AuthenticationException("Invalid username or password");
        }
        return user.get();
    }

    public List<User> listAll() {
        return repository.findAllUsers().stream()
                .sorted(Comparator.comparing(User::username))
                .toList();
    }

    public Optional<User> find(UUID id) {
        return repository.findUserById(id);
    }

    public User get(UUID id) {
        return find(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User getByUsername(String rawUsername) {
        String username = Validation.username(rawUsername);
        return repository.findUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '" + username + "' not found"));
    }
}
