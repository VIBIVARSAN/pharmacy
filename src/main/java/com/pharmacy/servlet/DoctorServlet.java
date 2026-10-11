package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Doctor;
import com.pharmacy.model.User;
import com.pharmacy.service.DoctorService;
import com.pharmacy.util.PageRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/doctors/*")
public class DoctorServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/doctors";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        DoctorService service = app().getDoctorService();
        switch (path) {
            case "" -> {
                boolean includeInactive = "1".equals(req.getParameter("inactive"));
                req.setAttribute("page", service.search(user, pageRequest(req), includeInactive));
                req.setAttribute("includeInactive", includeInactive);
                render(req, resp, "doctors/list");
            }
            case "/new" -> {
                req.setAttribute("doctor", new Doctor());
                render(req, resp, "doctors/form");
            }
            case "/edit" -> {
                req.setAttribute("doctor", service.get(user, requiredId(req, "id")));
                render(req, resp, "doctors/form");
            }
            case "/view" -> {
                Doctor doctor = service.get(user, requiredId(req, "id"));
                req.setAttribute("doctor", doctor);
                req.setAttribute("prescriptions", app().getPrescriptionService()
                        .search(user, PageRequest.firstPage(20), null, null, doctor.getId(), null, null));
                render(req, resp, "doctors/view");
            }
            default -> super.get(path, req, resp);
        }
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        DoctorService service = app().getDoctorService();
        switch (path) {
            case "/save" -> save(req, resp, user, service);
            case "/deactivate", "/reactivate" -> {
                long id = requiredId(req, "id");
                boolean activate = path.equals("/reactivate");
                service.setActive(user, id, activate);
                flashSuccess(req, activate ? "Doctor reactivated." : "Doctor deactivated. Existing prescriptions are unaffected.");
                redirect(req, resp, "/doctors/view?id=" + id);
            }
            default -> super.post(path, req, resp);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, DoctorService service) throws Exception {
        Doctor d = new Doctor();
        d.setId(longParam(req, "id"));
        d.setFullName(str(req, "fullName"));
        d.setSpecialization(str(req, "specialization"));
        d.setPhone(str(req, "phone"));
        d.setEmail(str(req, "email"));
        d.setHospital(str(req, "hospital"));
        d.setRegistrationNo(str(req, "registrationNo"));
        try {
            if (d.isNew()) {
                service.create(user, d);
                flashSuccess(req, "Doctor added with ID " + d.getDoctorCode() + ".");
            } else {
                service.update(user, d);
                flashSuccess(req, "Doctor details updated.");
            }
            redirect(req, resp, "/doctors/view?id=" + d.getId());
        } catch (ValidationException e) {
            if (!d.isNew()) {
                d.setDoctorCode(service.get(user, d.getId()).getDoctorCode());
            }
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("doctor", d);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(req, resp, "doctors/form");
        }
    }
}
