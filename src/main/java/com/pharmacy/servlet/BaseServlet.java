package com.pharmacy.servlet;

import com.pharmacy.exception.AppException;
import com.pharmacy.exception.AuthorizationException;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.DataAccessException;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.filter.AppInitListener;
import com.pharmacy.filter.AuthFilter;
import com.pharmacy.model.User;
import com.pharmacy.service.AppContext;
import com.pharmacy.util.JsonWriter;
import com.pharmacy.util.PageRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parent of all page controllers (template-method pattern).
 * Subclasses override {@link #get} and/or {@link #post} and receive the part
 * of the URL after the servlet mapping. This class supplies everything the
 * controllers have in common: centralized exception handling, flash
 * messages, parameter parsing, paging and view rendering.
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());
    private static final String FLASH = "flash";

    // ------------------------------------------------------------ dispatching

    /** Handles a GET for the given sub-path ("" for the servlet root). */
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /** Handles a POST for the given sub-path. */
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /** Where to send the user after a failed POST when the form did not say otherwise. */
    protected String homePath() {
        return "/dashboard";
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, false);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, true);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp, boolean isPost) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            path = "";
        }
        try {
            if (isPost) {
                post(path, req, resp);
            } else {
                get(path, req, resp);
            }
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (AuthorizationException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (ValidationException | BusinessRuleException e) {
            // Expected, user-correctable problems: explain them and go back to the page.
            if (isPost && !resp.isCommitted()) {
                flashError(req, e.getMessage());
                redirect(req, resp, returnPath(req, homePath()));
            } else {
                resp.sendError(e.getHttpStatus(), e.getMessage());
            }
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Database error: " + req.getMethod() + " " + req.getRequestURI(), e);
            if (e.isTransient() && isPost && !resp.isCommitted()) {
                // Two users changed the same records at once; nothing was saved, so it is safe to try again.
                flashError(req, "The record was being changed by someone else at the same moment. Nothing was saved - please try again.");
                redirect(req, resp, returnPath(req, homePath()));
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (AppException e) {
            LOG.log(Level.SEVERE, "Request failed: " + req.getMethod() + " " + req.getRequestURI(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (ServletException | IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // ---------------------------------------------------------------- context

    protected AppContext app() {
        return (AppContext) getServletContext().getAttribute(AppInitListener.APP_CONTEXT);
    }

    protected User currentUser(HttpServletRequest req) {
        return (User) req.getAttribute(AuthFilter.REQUEST_USER);
    }

    // ---------------------------------------------------------------- output

    /** Forwards to a JSP under /WEB-INF/views (which browsers cannot request directly). */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(FLASH) != null) {
            req.setAttribute(FLASH, session.getAttribute(FLASH));
            session.removeAttribute(FLASH);
        }
        if (app() != null && currentUser(req) != null) {
            req.setAttribute("settings", app().getSettingsService().asMap());
        }
        resp.setContentType("text/html;charset=UTF-8");
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    /** Post/Redirect/Get: {@code path} is relative to the application context. */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected void json(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonWriter.toJson(body));
    }

    protected void flashSuccess(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH, Map.of("type", "success", "message", message));
    }

    protected void flashError(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH, Map.of("type", "danger", "message", message));
    }

    protected void flashInfo(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH, Map.of("type", "info", "message", message));
    }

    /**
     * The local page a form asked to return to (hidden field "returnTo").
     * Only paths inside this application are accepted, which rules out open redirects.
     */
    protected String returnPath(HttpServletRequest req, String fallback) {
        String target = req.getParameter("returnTo");
        if (target != null && target.startsWith("/") && !target.startsWith("//") && !target.contains("\\")
                && !target.contains("\r") && !target.contains("\n")) {
            return target;
        }
        return fallback;
    }

    // ------------------------------------------------------------- parameters

    protected static String str(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? null : v.trim();
    }

    protected static Long longParam(HttpServletRequest req, String name) {
        return parseLong(req.getParameter(name));
    }

    protected static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static Integer intParam(HttpServletRequest req, String name) {
        return parseInt(req.getParameter(name));
    }

    protected static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static BigDecimal decimalParam(HttpServletRequest req, String name) {
        return parseDecimal(req.getParameter(name));
    }

    protected static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal d = new BigDecimal(value.trim());
            return d.scale() > 2 ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static LocalDate dateParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    protected static <E extends Enum<E>> E enumParam(HttpServletRequest req, String name, Class<E> type) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, v.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The id of the record a URL refers to; a missing or malformed id is a 404. */
    protected static long requiredId(HttpServletRequest req, String name) {
        Long id = longParam(req, name);
        if (id == null) {
            throw new NotFoundException("The requested record");
        }
        return id;
    }

    /**
     * Reads page / size / sort / dir / q and publishes helper attributes the
     * pagination and sort-header tags use to build links that keep the
     * current filters.
     */
    protected PageRequest pageRequest(HttpServletRequest req) {
        Integer page = intParam(req, "page");
        Integer size = intParam(req, "size");
        String sort = str(req, "sort");
        boolean desc = "desc".equalsIgnoreCase(req.getParameter("dir"));
        PageRequest pr = new PageRequest(page == null ? 1 : page, size == null ? PageRequest.DEFAULT_SIZE : size,
                sort, desc, str(req, "q"));
        req.setAttribute("pageRequest", pr);
        req.setAttribute("qsNoPage", queryString(req, Set.of("page")));
        req.setAttribute("qsNoSort", queryString(req, Set.of("page", "sort", "dir")));
        return pr;
    }

    private static String queryString(HttpServletRequest req, Set<String> exclude) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            if (exclude.contains(e.getKey())) {
                continue;
            }
            for (String value : e.getValue()) {
                if (value != null && !value.isEmpty()) {
                    sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append("&amp;");
                }
            }
        }
        return sb.toString();
    }
}
