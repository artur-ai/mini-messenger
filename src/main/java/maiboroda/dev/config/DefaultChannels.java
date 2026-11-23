package maiboroda.dev.config;

import maiboroda.dev.service.ConversationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultChannels implements ApplicationRunner {

    private static final List<String> CHANNELS = List.of(
            "#general", "#kitchen", "#laundry", "#lost-and-found", "#study", "#announcements");

    private final ConversationService conversationService;

    public DefaultChannels(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        conversationService.ensureChannelsExist(CHANNELS);
    }
}
