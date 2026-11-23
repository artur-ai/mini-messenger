package maiboroda.dev.web;

import maiboroda.dev.service.MessageService;
import maiboroda.dev.web.Views.MessageView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MessageController {

    public record SendMessageRequest(String text) {
    }

    private final MessageService messageService;
    private final Views views;

    public MessageController(MessageService messageService, Views views) {
        this.messageService = messageService;
        this.views = views;
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageView> history(AuthUser me,
                                     @PathVariable UUID id,
                                     @RequestParam(defaultValue = "0") long after,
                                     @RequestParam(defaultValue = "" + MessageService.DEFAULT_PAGE_SIZE) int limit) {
        return messageService.history(me.id(), id, after, limit).stream().map(views::message).toList();
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageView send(AuthUser me, @PathVariable UUID id, @RequestBody SendMessageRequest request) {
        return views.message(messageService.send(me.id(), id, request.text()));
    }

    @GetMapping("/messages/search")
    public List<MessageView> search(AuthUser me, @RequestParam("q") String query) {
        return messageService.search(me.id(), query).stream().map(views::message).toList();
    }
}
