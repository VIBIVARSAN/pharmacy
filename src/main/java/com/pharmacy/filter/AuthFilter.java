package com.pharmacy.filter;

import com.pharmacy.model.Permission;
import com.pharmacy.model.User;
import com.pharmacy.service.AppContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication and coarse role checks for every request.
 * <ul>
 *   <li>Static assets, /login and /setup are public; everything else needs a signed-in, active user.</li>
 *   <li>Administrator-only areas are blocked here by URL; the services check fine-grained permissions again.</li>
 *   <li>The user is re-read from the database on each request, so deactivating an
 *       account or changing its role takes effect immediately.</li>
 * </ul>
 */
public class AuthFilter implements Filter {
    public static final String SESSION_USER_ID = "authUserId";
    public static final String REQUEST_USER = "authUser";

    /** URL prefix -> permission required to enter that area at all. */
    private static final Map<String, Permission> PROTECTED_AREAS = Map.of(
            "/users", Permission.USER_MANAGE,
            "/audit-logs", Permission.AUDIT_VIEW,
            "/settings", Permission.SETTINGS_MANAGE,
            "/reports", Permission.REPORT_VIEW);
    private static final List<String> PUBLIC_PATHS = List.of("/login", "/setup");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());

        if (path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }

        // Pages with personal data must not be kept in browser or proxy caches.
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Pragma", "no-cache");

        AppContext app = (AppContext) req.getServletContext().getAttribute(AppInitListener.APP_CONTEXT);
        if (app == null) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            req.getRequestDispatcher("/WEB-INF/views/error/config.jsp").forward(req, resp);
            return;
        }

        User user = resolveUser(req, app);
        boolean isPublic = PUBLIC_PATHS.contains(path);

        if (user == null) {
            if (isPublic) {
                chain.doFilter(request, response);
            } else if (path.startsWith("/api/")) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\":\"Your session has expired. Please sign in again.\"}");
            } else {
                resp.sendRedirect(req.getContextPath() + (app.getAuthService().isSetupRequired() ? "/setup" : "/login"));
            }
            return;
        }

        if (isPublic) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        for (Map.Entry<String, Permission> area : PROTECTED_AREAS.entrySet()) {
            boolean inside = path.equals(area.getKey()) || path.startsWith(area.getKey() + "/");
            if (inside && !user.can(area.getValue())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        req.setAttribute(REQUEST_USER, user);
        Map<String, Boolean> permissions = new HashMap<>();
        for (Permission p : Permission.values()) {
            permissions.put(p.name(), user.can(p));
        }
        req.setAttribute("perm", permissions);
        chain.doFilter(request, response);
    }

    private User resolveUser(HttpServletRequest req, AppContext app) {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_USER_ID) instanceof Long userId)) {
            return null;
        }
        Optional<User> user = app.getAuthService().findActiveUser(userId);
        if (user.isEmpty()) {
            session.invalidate(); // account was deactivated or removed
            return null;
        }
        return user.get();
    }
}
