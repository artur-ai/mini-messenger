package maiboroda.dev.web;

import maiboroda.dev.service.UserService;
import maiboroda.dev.web.Views.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final Views views;

    public UserController(UserService userService, Views views) {
        this.userService = userService;
        this.views = views;
    }

    @GetMapping
    public List<UserView> list(AuthUser me) {
        return userService.listAll().stream().map(views::user).toList();
    }
}
