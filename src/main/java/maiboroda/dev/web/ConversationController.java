package maiboroda.dev.web;

import maiboroda.dev.model.Conversation;
import maiboroda.dev.service.ConversationService;
import maiboroda.dev.web.Views.ConversationView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ConversationController {

    public record CreateChannelRequest(String name, List<String> members) {
    }

    public record DirectRequest(String username) {
    }

    private final ConversationService conversationService;
    private final Views views;

    public ConversationController(ConversationService conversationService, Views views) {
        this.conversationService = conversationService;
        this.views = views;
    }

    @GetMapping("/conversations")
    public List<ConversationView> mine(AuthUser me) {
        return conversationService.listFor(me.id()).stream()
                .map(c -> views.conversation(c, me.id()))
                .toList();
    }

    @GetMapping("/channels")
    public List<ConversationView> channels(AuthUser me) {
        return conversationService.listChannels().stream()
                .map(c -> views.conversation(c, me.id()))
                .toList();
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationView createChannel(AuthUser me, @RequestBody CreateChannelRequest request) {
        Conversation channel = conversationService.createChannel(me.id(), request.name(), request.members());
        return views.conversation(channel, me.id());
    }

    @PostMapping("/channels/{name}/join")
    public ConversationView join(AuthUser me, @PathVariable String name) {
        return views.conversation(conversationService.join(me.id(), name), me.id());
    }

    @PostMapping("/direct")
    public ConversationView direct(AuthUser me, @RequestBody DirectRequest request) {
        return views.conversation(conversationService.openDirect(me.id(), request.username()), me.id());
    }
}
