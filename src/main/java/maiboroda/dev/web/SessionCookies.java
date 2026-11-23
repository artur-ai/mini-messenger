package maiboroda.dev.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import maiboroda.dev.service.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SessionCookies {

    public static final String NAME = "dorm3_session";

    private final SessionService sessionService;
    private final boolean secure;

    public SessionCookies(SessionService sessionService, @Value("${messenger.cookie-secure:false}") boolean secure) {
        this.sessionService = sessionService;
        this.secure = secure;
    }

    public ResponseCookie create(String token) {
        return build(token, sessionService.ttl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
