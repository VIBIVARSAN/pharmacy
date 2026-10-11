package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** "My profile": shows the signed-in user's details and lets them change their own password. */
@WebServlet("/profile")
public class ProfileServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        render(req, resp, "profile");
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            app().getUserService().changeOwnPassword(currentUser(req), req.getParameter("currentPassword"),
                    req.getParameter("newPassword"), req.getParameter("confirmPassword"));
            flashSuccess(req, "Your password has been changed.");
            redirect(req, resp, "/profile");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, "profile");
        }
    }
}
