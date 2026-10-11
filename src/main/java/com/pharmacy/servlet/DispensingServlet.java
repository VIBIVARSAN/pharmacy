package com.pharmacy.servlet;

import com.pharmacy.exception.DuplicateOperationException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Dispensing;
import com.pharmacy.model.Prescription;
import com.pharmacy.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@WebServlet("/dispensing/*")
public class DispensingServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/dispensing";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        switch (path) {
            // Queue of prescriptions that are issued or only partly dispensed.
            case "" -> {
                req.setAttribute("page", app().getPrescriptionService()
                        .search(user, pageRequest(req), "DISPENSABLE", null, null, null, null));
                render(req, resp, "dispensing/queue");
            }
            case "/history" -> {
                req.setAttribute("page", app().getDispensingService().search(user, pageRequest(req), str(req, "billing"),
                        dateParam(req, "from"), dateParam(req, "to")));
                render(req, resp, "dispensing/history");
            }
            case "/new" -> {
                Prescription p = app().getPrescriptionService().get(user, requiredId(req, "prescriptionId"));
                if (!p.getStatus().isDispensable()) {
                    flashError(req, "Prescription " + p.getPrescriptionNo() + " is " + p.getStatus().getLabel().toLowerCase()
                            + " and cannot be dispensed.");
                    redirect(req, resp, "/prescriptions/view?id=" + p.getId());
                    return;
                }
                req.setAttribute("prescription", p);
                // One token per rendered form: submitting the same form twice is detected by the server.
                req.setAttribute("requestToken", UUID.randomUUID().toString().replace("-", ""));
                render(req, resp, "dispensing/form");
            }
            case "/view" -> {
                req.setAttribute("dispensing", app().getDispensingService().get(user, requiredId(req, "id")));
                render(req, resp, "dispensing/view");
            }
            default -> super.get(path, req, resp);
        }
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (!path.equals("/save")) {
            super.post(path, req, resp);
            return;
        }
        User user = currentUser(req);
        long prescriptionId = requiredId(req, "prescriptionId");
        Map<Long, Integer> quantities = new HashMap<>();
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!name.startsWith("qty_")) {
                continue;
            }
            Long itemId = parseLong(name.substring(4));
            String raw = req.getParameter(name);
            if (itemId == null || raw == null || raw.isBlank()) {
                continue;
            }
            Integer qty = parseInt(raw);
            if (qty == null) {
                throw new ValidationException(name, "Quantities must be whole numbers.");
            }
            quantities.put(itemId, qty);
        }
        try {
            Dispensing d = app().getDispensingService().dispense(user, prescriptionId, quantities,
                    req.getParameter("requestToken"), str(req, "notes"));
            flashSuccess(req, "Medicines dispensed (" + d.getDispensingNo() + "). Stock has been updated.");
            redirect(req, resp, "/dispensing/view?id=" + d.getId());
        } catch (DuplicateOperationException e) {
            flashInfo(req, e.getMessage());
            redirect(req, resp, e.getExistingId() == null ? "/dispensing/history" : "/dispensing/view?id=" + e.getExistingId());
        }
    }
}
