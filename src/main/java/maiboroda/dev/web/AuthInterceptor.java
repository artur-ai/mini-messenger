package maiboroda.dev.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import maiboroda.dev.service.AuthenticationException;
import maiboroda.dev.service.SessionService;
import maiboroda.dev.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    static final String ATTRIBUTE = AuthUser.class.getName();

    private final SessionService sessionService;
    private final UserService userService;
    private final SessionCookies cookies;

    public AuthInterceptor(SessionService sessionService, UserService userService, SessionCookies cookies) {
        this.sessionService = sessionService;
        this.userService = userService;
        this.cookies = cookies;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuthUser user = sessionService.resolve(cookies.read(request))
                .flatMap(userService::find)
                .map(u -> new AuthUser(u.id(), u.username()))
                .orElseThrow(() -> new AuthenticationException("Please log in"));
        request.setAttribute(ATTRIBUTE, user);
        return true;
    }
}
