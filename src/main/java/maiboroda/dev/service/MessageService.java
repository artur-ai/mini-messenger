package maiboroda.dev.service;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.model.Message;
import maiboroda.dev.storage.ChatRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MessageService {

    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 500;
    public static final int MAX_SEARCH_RESULTS = 50;

    private final ChatRepository repository;
    private final ConversationService conversationService;
    private final Clock clock;

    public MessageService(ChatRepository repository, ConversationService conversationService, Clock clock) {
        this.repository = repository;
        this.conversationService = conversationService;
        this.clock = clock;
    }

    public synchronized Message send(UUID senderId, UUID conversationId, String rawText) {
        conversationService.requireParticipant(conversationId, senderId);
        String text = Validation.messageText(rawText);
        long seq = repository.lastSeq(conversationId) + 1;
        Message message = new Message(UUID.randomUUID(), conversationId, senderId, seq, text, clock.instant());
        repository.saveMessage(message);
        return message;
    }

    public List<Message> history(UUID userId, UUID conversationId, long afterSeq, int limit) {
        conversationService.requireParticipant(conversationId, userId);
        int size = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        List<Message> all = repository.findMessagesByConversation(conversationId);
        if (afterSeq > 0) {
            return all.stream().filter(m -> m.seq() > afterSeq).limit(size).toList();
        }
        return all.subList(Math.max(0, all.size() - size), all.size());
    }

    public List<Message> search(UUID userId, String rawQuery) {
        String query = Validation.searchQuery(rawQuery).toLowerCase(Locale.ROOT);
        return conversationService.listFor(userId).stream()
                .map(Conversation::id)
                .flatMap(id -> repository.findMessagesByConversation(id).stream())
                .filter(m -> m.text().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Message.CHRONOLOGICAL.reversed())
                .limit(MAX_SEARCH_RESULTS)
                .toList();
    }
}
