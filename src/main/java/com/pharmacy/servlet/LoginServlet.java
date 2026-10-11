package com.pharmacy.servlet;

import com.pharmacy.exception.AuthenticationException;
import com.pharmacy.filter.AuthFilter;
import com.pharmacy.filter.CsrfFilter;
import com.pharmacy.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (app().getAuthService().isSetupRequired()) {
            redirect(req, resp, "/setup");
            return;
        }
        render(req, resp, "auth/login");
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String username = str(req, "username");
        try {
            User user = app().getAuthService().authenticate(username, req.getParameter("password"));
            // New session after login: prevents session fixation and rotates the CSRF token.
            req.getSession().invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute(AuthFilter.SESSION_USER_ID, user.getId());
            CsrfFilter.issueToken(session);
            redirect(req, resp, "/dashboard");
        } catch (AuthenticationException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("username", username);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            render(req, resp, "auth/login");
        }
    }
}
