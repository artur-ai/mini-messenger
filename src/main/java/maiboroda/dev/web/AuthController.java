package maiboroda.dev.web;

import jakarta.servlet.http.HttpServletRequest;
import maiboroda.dev.model.User;
import maiboroda.dev.service.SessionService;
import maiboroda.dev.service.UserService;
import maiboroda.dev.web.Views.UserView;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record Credentials(String username, String password) {
    }

    private final UserService userService;
    private final SessionService sessionService;
    private final SessionCookies cookies;
    private final Views views;

    public AuthController(UserService userService, SessionService sessionService, SessionCookies cookies, Views views) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.cookies = cookies;
        this.views = views;
    }

    @PostMapping("/register")
    public ResponseEntity<UserView> register(@RequestBody Credentials credentials) {
        User user = userService.register(credentials.username(), credentials.password());
        return startSession(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<UserView> login(@RequestBody Credentials credentials) {
        User user = userService.authenticate(credentials.username(), credentials.password());
        return startSession(user, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionService.invalidate(cookies.read(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public UserView me(AuthUser me) {
        return views.user(userService.get(me.id()));
    }

    private ResponseEntity<UserView> startSession(User user, HttpStatus status) {
        String token = sessionService.create(user.id());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookies.create(token).toString())
                .body(views.user(user));
    }
}
