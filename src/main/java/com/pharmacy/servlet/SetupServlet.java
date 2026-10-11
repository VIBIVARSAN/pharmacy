package com.pharmacy.servlet;

import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** One-time creation of the first administrator. Disabled as soon as any user exists. */
@WebServlet("/setup")
public class SetupServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (!app().getAuthService().isSetupRequired()) {
            redirect(req, resp, "/login");
            return;
        }
        render(req, resp, "auth/setup");
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (!app().getAuthService().isSetupRequired()) {
            redirect(req, resp, "/login");
            return;
        }
        try {
            app().getAuthService().createFirstAdmin(req.getParameter("token"), str(req, "username"), str(req, "fullName"),
                    req.getParameter("password"), req.getParameter("confirmPassword"));
            flashSuccess(req, "Administrator account created. You can now sign in.");
            redirect(req, resp, "/login");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("username", str(req, "username"));
            req.setAttribute("fullName", str(req, "fullName"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, "auth/setup");
        } catch (BusinessRuleException e) {
            flashInfo(req, e.getMessage());
            redirect(req, resp, "/login");
        }
    }
}
