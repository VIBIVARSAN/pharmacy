package com.pharmacy.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Synchronizer-token CSRF protection. A random token is kept in the session
 * and must come back with every state-changing request, either as the hidden
 * form field "_csrf" or as the "X-CSRF-Token" header. Only GET/HEAD/OPTIONS
 * are allowed without it, and those never change data in this application.
 */
public class CsrfFilter implements Filter {
    public static final String TOKEN_ATTRIBUTE = "csrfToken";
    public static final String PARAMETER = "_csrf";
    public static final String HEADER = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Creates a fresh token for the session (also called after login to rotate it). */
    public static String issueToken(HttpSession session) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(TOKEN_ATTRIBUTE, token);
        return token;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (req.getServletPath().startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(true);
        String expected = (String) session.getAttribute(TOKEN_ATTRIBUTE);
        if (expected == null) {
            expected = issueToken(session);
        }

        if (!SAFE_METHODS.contains(req.getMethod())) {
            if (!"POST".equals(req.getMethod())) {
                resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            String supplied = req.getParameter(PARAMETER);
            if (supplied == null) {
                supplied = req.getHeader(HEADER);
            }
            if (supplied == null || !MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                    expected.getBytes(StandardCharsets.UTF_8))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Your session has expired or the form is no longer valid. Go back, reload the page and try again.");
                return;
            }
        }
        req.setAttribute(TOKEN_ATTRIBUTE, expected);
        chain.doFilter(request, response);
    }
}
