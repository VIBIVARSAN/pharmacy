package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Supplier;
import com.pharmacy.model.User;
import com.pharmacy.service.SupplierService;
import com.pharmacy.util.PageRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/suppliers/*")
public class SupplierServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/suppliers";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        SupplierService service = app().getSupplierService();
        switch (path) {
            case "" -> {
                boolean includeInactive = "1".equals(req.getParameter("inactive"));
                req.setAttribute("page", service.search(user, pageRequest(req), includeInactive));
                req.setAttribute("includeInactive", includeInactive);
                render(req, resp, "suppliers/list");
            }
            case "/new" -> {
                req.setAttribute("supplier", new Supplier());
                render(req, resp, "suppliers/form");
            }
            case "/edit" -> {
                req.setAttribute("supplier", service.get(user, requiredId(req, "id")));
                render(req, resp, "suppliers/form");
            }
            case "/view" -> {
                Supplier supplier = service.get(user, requiredId(req, "id"));
                req.setAttribute("supplier", supplier);
                req.setAttribute("batches", app().getInventoryService()
                        .searchBatches(user, PageRequest.firstPage(50), null, supplier.getId()));
                render(req, resp, "suppliers/view");
            }
            default -> super.get(path, req, resp);
        }
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        SupplierService service = app().getSupplierService();
        switch (path) {
            case "/save" -> save(req, resp, user, service);
            case "/deactivate", "/reactivate" -> {
                long id = requiredId(req, "id");
                boolean activate = path.equals("/reactivate");
                service.setActive(user, id, activate);
                flashSuccess(req, activate ? "Supplier reactivated." : "Supplier deactivated. Past stock receipts are unaffected.");
                redirect(req, resp, "/suppliers/view?id=" + id);
            }
            default -> super.post(path, req, resp);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, SupplierService service) throws Exception {
        Supplier s = new Supplier();
        s.setId(longParam(req, "id"));
        s.setName(str(req, "name"));
        s.setContactPerson(str(req, "contactPerson"));
        s.setPhone(str(req, "phone"));
        s.setEmail(str(req, "email"));
        s.setAddress(str(req, "address"));
        try {
            if (s.isNew()) {
                service.create(user, s);
                flashSuccess(req, "Supplier added with ID " + s.getSupplierCode() + ".");
            } else {
                service.update(user, s);
                flashSuccess(req, "Supplier details updated.");
            }
            redirect(req, resp, "/suppliers/view?id=" + s.getId());
        } catch (ValidationException e) {
            if (!s.isNew()) {
                s.setSupplierCode(service.get(user, s.getId()).getSupplierCode());
            }
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("supplier", s);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, "suppliers/form");
        }
    }
}
