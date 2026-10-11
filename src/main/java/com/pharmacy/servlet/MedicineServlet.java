package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.User;
import com.pharmacy.service.MedicineService;
import com.pharmacy.service.SettingsService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/medicines/*")
public class MedicineServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/medicines";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        MedicineService service = app().getMedicineService();
        switch (path) {
            case "" -> {
                boolean includeInactive = "1".equals(req.getParameter("inactive"));
                req.setAttribute("page", service.search(user, pageRequest(req), str(req, "category"), str(req, "stock"), includeInactive));
                req.setAttribute("categories", service.getCategories(user));
                req.setAttribute("includeInactive", includeInactive);
                render(req, resp, "medicines/list");
            }
            case "/new" -> {
                Medicine m = new Medicine();
                m.setReorderLevel(app().getSettingsService().getInt(SettingsService.DEFAULT_REORDER, 10));
                req.setAttribute("medicine", m);
                req.setAttribute("reorderLevelValue", String.valueOf(m.getReorderLevel()));
                req.setAttribute("categories", service.getCategories(user));
                render(req, resp, "medicines/form");
            }
            case "/edit" -> {
                Medicine m = service.get(user, requiredId(req, "id"));
                req.setAttribute("medicine", m);
                req.setAttribute("reorderLevelValue", String.valueOf(m.getReorderLevel()));
                req.setAttribute("categories", service.getCategories(user));
                render(req, resp, "medicines/form");
            }
            case "/view" -> {
                Medicine m = service.get(user, requiredId(req, "id"));
                req.setAttribute("medicine", m);
                req.setAttribute("batches", app().getInventoryService().getBatches(user, m.getId()));
                req.setAttribute("transactions", app().getInventoryService().getRecentTransactions(user, m.getId(), 15));
                req.setAttribute("nearExpiryDays", app().getSettingsService().getNearExpiryDays());
                render(req, resp, "medicines/view");
            }
            default -> super.get(path, req, resp);
        }
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        MedicineService service = app().getMedicineService();
        switch (path) {
            case "/save" -> save(req, resp, user, service);
            case "/deactivate", "/reactivate" -> {
                long id = requiredId(req, "id");
                boolean activate = path.equals("/reactivate");
                service.setActive(user, id, activate);
                flashSuccess(req, activate ? "Medicine reactivated." : "Medicine deactivated. Its history is kept.");
                redirect(req, resp, "/medicines/view?id=" + id);
            }
            default -> super.post(path, req, resp);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, MedicineService service) throws Exception {
        Medicine m = new Medicine();
        m.setId(longParam(req, "id"));
        m.setName(str(req, "name"));
        m.setGenericName(str(req, "genericName"));
        m.setCategory(str(req, "category"));
        m.setManufacturer(str(req, "manufacturer"));
        m.setDosageForm(str(req, "dosageForm"));
        m.setStrength(str(req, "strength"));
        m.setUnit(str(req, "unit"));
        m.setDescription(str(req, "description"));
        Integer reorderLevel = intParam(req, "reorderLevel");
        try {
            if (m.isNew()) {
                service.create(user, m, reorderLevel);
                flashSuccess(req, "Medicine added with ID " + m.getMedicineCode() + ". Receive stock to make it available.");
            } else {
                service.update(user, m, reorderLevel);
                flashSuccess(req, "Medicine details updated.");
            }
            redirect(req, resp, "/medicines/view?id=" + m.getId());
        } catch (ValidationException e) {
            if (!m.isNew()) {
                m.setMedicineCode(service.get(user, m.getId()).getMedicineCode());
            }
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("medicine", m);
            req.setAttribute("reorderLevelValue", req.getParameter("reorderLevel"));
            req.setAttribute("categories", service.getCategories(user));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, "medicines/form");
        }
    }
}
