package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Role;
import com.pharmacy.model.User;
import com.pharmacy.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/users/*")
public class UserServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/users";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        UserService service = app().getUserService();
        switch (path) {
            case "" -> {
                req.setAttribute("page", service.search(user, pageRequest(req)));
                render(req, resp, "users/list");
            }
            case "/new" -> {
                User blank = new User();
                blank.setRole(Role.PHARMACIST);
                showForm(req, resp, blank);
            }
            case "/edit" -> showForm(req, resp, service.get(user, requiredId(req, "id")));
            default -> super.get(path, req, resp);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, User formUser) throws Exception {
        req.setAttribute("formUser", formUser);
        req.setAttribute("roles", Role.values());
        render(req, resp, "users/form");
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        UserService service = app().getUserService();
        switch (path) {
            case "/save" -> {
                User u = new User();
                u.setId(longParam(req, "id"));
                u.setUsername(str(req, "username"));
                u.setFullName(str(req, "fullName"));
                u.setEmail(str(req, "email"));
                u.setPhone(str(req, "phone"));
                u.setRole(enumParam(req, "role", Role.class));
                u.setActive(u.isNew() || "true".equals(req.getParameter("active")));
                try {
                    if (u.isNew()) {
                        service.create(user, u, req.getParameter("password"), req.getParameter("confirmPassword"));
                        flashSuccess(req, "User " + u.getUsername() + " created.");
                    } else {
                        service.update(user, u);
                        flashSuccess(req, "User updated.");
                    }
                    redirect(req, resp, "/users");
                } catch (ValidationException e) {
                    if (!u.isNew()) {
                        u.setUsername(service.get(user, u.getId()).getUsername());
                    }
                    req.setAttribute("errors", e.getErrors());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    showForm(req, resp, u);
                }
            }
            case "/reset-password" -> {
                long id = requiredId(req, "id");
                service.resetPassword(user, id, req.getParameter("password"), req.getParameter("confirmPassword"));
                flashSuccess(req, "Password has been reset.");
                redirect(req, resp, "/users/edit?id=" + id);
            }
            default -> super.post(path, req, resp);
        }
    }
}
