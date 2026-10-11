package com.pharmacy.servlet;

import com.pharmacy.model.User;
import com.pharmacy.service.AuditService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/audit-logs")
public class AuditLogServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        AuditService service = app().getAuditService();
        req.setAttribute("page", service.search(user, pageRequest(req), str(req, "action"), str(req, "entityType"),
                dateParam(req, "from"), dateParam(req, "to")));
        req.setAttribute("actions", service.getActions(user));
        req.setAttribute("entityTypes", service.getEntityTypes(user));
        render(req, resp, "audit/list");
    }
}
