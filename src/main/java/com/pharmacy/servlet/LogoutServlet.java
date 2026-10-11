package com.pharmacy.servlet;

import com.pharmacy.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Logout is a POST (protected by the CSRF token) so that a link on another site cannot sign a user out. */
@WebServlet("/logout")
public class LogoutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        if (user != null) {
            app().getAuthService().recordLogout(user);
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirect(req, resp, "/login?loggedOut=1");
    }
}
