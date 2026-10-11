package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Gender;
import com.pharmacy.model.Patient;
import com.pharmacy.model.User;
import com.pharmacy.service.PatientService;
import com.pharmacy.util.PageRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/patients/*")
public class PatientServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/patients";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        PatientService service = app().getPatientService();
        switch (path) {
            case "" -> {
                boolean includeInactive = "1".equals(req.getParameter("inactive"));
                req.setAttribute("page", service.search(user, pageRequest(req), includeInactive));
                req.setAttribute("includeInactive", includeInactive);
                render(req, resp, "patients/list");
            }
            case "/new" -> {
                req.setAttribute("patient", new Patient());
                render(req, resp, "patients/form");
            }
            case "/edit" -> {
                req.setAttribute("patient", service.get(user, requiredId(req, "id")));
                render(req, resp, "patients/form");
            }
            case "/view" -> {
                Patient patient = service.get(user, requiredId(req, "id"));
                req.setAttribute("patient", patient);
                // history: latest prescriptions and invoices of this patient
                req.setAttribute("prescriptions", app().getPrescriptionService()
                        .search(user, PageRequest.firstPage(20), null, patient.getId(), null, null, null));
                req.setAttribute("invoices", app().getBillingService()
                        .search(user, PageRequest.firstPage(20), null, null, null, null, patient.getId()));
                render(req, resp, "patients/view");
            }
            default -> super.get(path, req, resp);
        }
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        PatientService service = app().getPatientService();
        switch (path) {
            case "/save" -> save(req, resp, user, service);
            case "/deactivate", "/reactivate" -> {
                long id = requiredId(req, "id");
                boolean activate = path.equals("/reactivate");
                service.setActive(user, id, activate);
                flashSuccess(req, activate ? "Patient reactivated." : "Patient deactivated.");
                redirect(req, resp, "/patients/view?id=" + id);
            }
            default -> super.post(path, req, resp);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, PatientService service) throws Exception {
        Patient p = new Patient();
        p.setId(longParam(req, "id"));
        p.setFullName(str(req, "fullName"));
        p.setDateOfBirth(dateParam(req, "dateOfBirth"));
        p.setGender(enumParam(req, "gender", Gender.class));
        p.setPhone(str(req, "phone"));
        p.setEmail(str(req, "email"));
        p.setAddress(str(req, "address"));
        try {
            if (p.isNew()) {
                service.create(user, p);
                flashSuccess(req, "Patient registered with ID " + p.getPatientCode() + ".");
            } else {
                service.update(user, p);
                flashSuccess(req, "Patient details updated.");
            }
            redirect(req, resp, "/patients/view?id=" + p.getId());
        } catch (ValidationException e) {
            if (!p.isNew()) {
                p.setPatientCode(service.get(user, p.getId()).getPatientCode());
            }
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("patient", p);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, "patients/form");
        }
    }
}
