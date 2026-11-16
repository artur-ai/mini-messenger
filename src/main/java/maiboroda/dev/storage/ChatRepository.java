package maiboroda.dev.storage;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.Message;
import maiboroda.dev.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository {

    List<User> findAllUsers();

    Optional<User> findUserById(UUID id);

    Optional<User> findUserByUsername(String username);

    void saveUser(User user);

    List<Conversation> findAllConversations();

    Optional<Conversation> findConversationById(UUID id);

    void saveConversation(Conversation conversation);

    List<Message> findMessagesByConversation(UUID conversationId);

    long lastSeq(UUID conversationId);

    void saveMessage(Message message);
}
