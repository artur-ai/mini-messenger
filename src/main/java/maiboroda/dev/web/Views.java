package maiboroda.dev.web;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.ConversationType;
import maiboroda.dev.model.Message;
import maiboroda.dev.model.User;
import maiboroda.dev.service.UserService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class Views {

    public record UserView(UUID id, String username, Instant createdAt) {
    }

    public record ConversationView(UUID id, ConversationType type, String title, List<String> members, boolean joined) {
    }

    public record MessageView(UUID id, UUID conversationId, long seq, String sender, String text, Instant createdAt) {
    }

    private final UserService userService;

    public Views(UserService userService) {
        this.userService = userService;
    }

    public UserView user(User user) {
        return new UserView(user.id(), user.username(), user.createdAt());
    }

    public ConversationView conversation(Conversation conversation, UUID viewerId) {
        List<String> members = conversation.participantIds().stream().map(this::username).sorted().toList();
        String title = conversation.type() == ConversationType.CHANNEL
                ? conversation.name()
                : conversation.participantIds().stream()
                        .filter(id -> !id.equals(viewerId))
                        .map(this::username)
                        .findFirst()
                        .orElse("?");
        return new ConversationView(conversation.id(), conversation.type(), title, members,
                conversation.hasParticipant(viewerId));
    }

    public MessageView message(Message message) {
        return new MessageView(message.id(), message.conversationId(), message.seq(),
                username(message.senderId()), message.text(), message.createdAt());
    }

    private String username(UUID id) {
        return userService.find(id).map(User::username).orElse("unknown");
    }
}
