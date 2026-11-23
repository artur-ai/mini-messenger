package maiboroda.dev.service;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.ConversationType;
import maiboroda.dev.model.User;
import maiboroda.dev.storage.ChatRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ConversationService {

    private static final Comparator<Conversation> DISPLAY_ORDER = Comparator
            .comparing((Conversation c) -> c.type() == ConversationType.CHANNEL ? 0 : 1)
            .thenComparing(c -> Objects.requireNonNullElse(c.name(), ""))
            .thenComparing(Conversation::createdAt);

    private final ChatRepository repository;
    private final UserService userService;
    private final Clock clock;

    public ConversationService(ChatRepository repository, UserService userService, Clock clock) {
        this.repository = repository;
        this.userService = userService;
        this.clock = clock;
    }

    public List<Conversation> listChannels() {
        return repository.findAllConversations().stream()
                .filter(c -> c.type() == ConversationType.CHANNEL)
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    public List<Conversation> listFor(UUID userId) {
        return repository.findAllConversations().stream()
                .filter(c -> c.hasParticipant(userId))
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    public synchronized Conversation createChannel(UUID creatorId, String rawName, Collection<String> inviteUsernames) {
        String name = Validation.channelName(rawName);
        if (findChannel(name).isPresent()) {
            throw new ConflictException("Channel " + name + " already exists");
        }
        Set<UUID> members = new LinkedHashSet<>();
        members.add(creatorId);
        if (inviteUsernames != null) {
            for (String username : inviteUsernames) {
                members.add(userService.getByUsername(username).id());
            }
        }
        Conversation channel = new Conversation(UUID.randomUUID(), ConversationType.CHANNEL, name,
                new ArrayList<>(members), clock.instant());
        repository.saveConversation(channel);
        return channel;
    }

    public synchronized void ensureChannelsExist(Collection<String> names) {
        for (String raw : names) {
            String name = Validation.channelName(raw);
            if (findChannel(name).isEmpty()) {
                repository.saveConversation(new Conversation(UUID.randomUUID(), ConversationType.CHANNEL, name,
                        List.of(), clock.instant()));
            }
        }
    }

    public synchronized Conversation join(UUID userId, String rawName) {
        String name = Validation.channelName(rawName);
        Conversation channel = findChannel(name)
                .orElseThrow(() -> new NotFoundException("Channel " + name + " not found"));
        Conversation updated = channel.withParticipant(userId);
        if (updated != channel) {
            repository.saveConversation(updated);
        }
        return updated;
    }

    public synchronized Conversation openDirect(UUID userId, String otherUsername) {
        User other = userService.getByUsername(otherUsername);
        if (other.id().equals(userId)) {
            throw new ValidationException("You cannot send a private message to yourself");
        }
        Optional<Conversation> existing = repository.findAllConversations().stream()
                .filter(c -> c.type() == ConversationType.DIRECT)
                .filter(c -> c.hasParticipant(userId) && c.hasParticipant(other.id()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        Conversation direct = new Conversation(UUID.randomUUID(), ConversationType.DIRECT, null,
                List.of(userId, other.id()), clock.instant());
        repository.saveConversation(direct);
        return direct;
    }

    public Conversation requireParticipant(UUID conversationId, UUID userId) {
        Conversation conversation = repository.findConversationById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        if (!conversation.hasParticipant(userId)) {
            throw new ForbiddenException("You are not a member of this conversation");
        }
        return conversation;
    }

    private Optional<Conversation> findChannel(String name) {
        return repository.findAllConversations().stream()
                .filter(c -> c.type() == ConversationType.CHANNEL && name.equals(c.name()))
                .findFirst();
    }
}
