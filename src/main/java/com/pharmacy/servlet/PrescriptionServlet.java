package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Doctor;
import com.pharmacy.model.Patient;
import com.pharmacy.model.Prescription;
import com.pharmacy.model.PrescriptionItem;
import com.pharmacy.model.PrescriptionStatus;
import com.pharmacy.model.User;
import com.pharmacy.service.PrescriptionService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;

@WebServlet("/prescriptions/*")
public class PrescriptionServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/prescriptions";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        PrescriptionService service = app().getPrescriptionService();
        switch (path) {
            case "" -> {
                PrescriptionStatus status = enumParam(req, "status", PrescriptionStatus.class);
                req.setAttribute("page", service.search(user, pageRequest(req), status == null ? null : status.name(),
                        null, null, dateParam(req, "from"), dateParam(req, "to")));
                req.setAttribute("statuses", PrescriptionStatus.values());
                render(req, resp, "prescriptions/list");
            }
            case "/new" -> {
                Prescription p = new Prescription();
                p.setPrescriptionDate(LocalDate.now());
                Long patientId = longParam(req, "patientId");
                if (patientId != null) {
                    p.setPatient(app().getPatientService().get(user, patientId));
                }
                showForm(req, resp, user, p);
            }
            case "/edit" -> showForm(req, resp, user, service.get(user, requiredId(req, "id")));
            case "/view", "/print" -> {
                Prescription p = service.get(user, requiredId(req, "id"));
                req.setAttribute("prescription", p);
                if (path.equals("/print")) {
                    req.setAttribute("settings", app().getSettingsService().asMap());
                    render(req, resp, "prescriptions/print");
                } else {
                    req.setAttribute("dispensings", app().getDispensingService().findByPrescription(user, p.getId()));
                    render(req, resp, "prescriptions/view");
                }
            }
            default -> super.get(path, req, resp);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, User user, Prescription p) throws Exception {
        req.setAttribute("prescription", p);
        req.setAttribute("medicines", app().getMedicineService().findAllActive(user));
        req.setAttribute("today", LocalDate.now().toString());
        render(req, resp, "prescriptions/form");
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        PrescriptionService service = app().getPrescriptionService();
        switch (path) {
            case "/save" -> save(req, resp, user, service);
            case "/issue" -> {
                long id = requiredId(req, "id");
                service.issue(user, id);
                flashSuccess(req, "Prescription issued. It can now be dispensed.");
                redirect(req, resp, "/prescriptions/view?id=" + id);
            }
            case "/cancel" -> {
                long id = requiredId(req, "id");
                service.cancel(user, id, str(req, "reason"));
                flashSuccess(req, "Prescription cancelled.");
                redirect(req, resp, "/prescriptions/view?id=" + id);
            }
            default -> super.post(path, req, resp);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, User user, PrescriptionService service) throws Exception {
        Prescription p = bind(req);
        boolean issue = "issue".equals(req.getParameter("action"));
        try {
            if (p.isNew()) {
                service.create(user, p, issue);
                flashSuccess(req, "Prescription " + p.getPrescriptionNo() + (issue ? " issued." : " saved as a draft."));
            } else {
                service.update(user, p, issue);
                flashSuccess(req, "Prescription updated.");
            }
            redirect(req, resp, "/prescriptions/view?id=" + p.getId());
        } catch (ValidationException e) {
            if (!p.isNew()) {
                Prescription existing = service.get(user, p.getId());
                p.setPrescriptionNo(existing.getPrescriptionNo());
                p.setStatus(existing.getStatus());
            }
            req.setAttribute("errors", e.getErrors());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showForm(req, resp, user, p);
        }
    }

    /** Builds a prescription from the form; item rows arrive as parallel arrays. */
    private Prescription bind(HttpServletRequest req) {
        Prescription p = new Prescription();
        p.setId(longParam(req, "id"));
        Long patientId = longParam(req, "patientId");
        if (patientId != null) {
            p.setPatient(new Patient(patientId, null, str(req, "patientLabel")));
        }
        Long doctorId = longParam(req, "doctorId");
        if (doctorId != null) {
            p.setDoctor(new Doctor(doctorId, null, str(req, "doctorLabel")));
        }
        p.setPrescriptionDate(dateParam(req, "prescriptionDate"));
        p.setDiagnosis(str(req, "diagnosis"));
        p.setNotes(str(req, "notes"));

        String[] medicineIds = values(req, "itemMedicineId");
        String[] dosages = values(req, "itemDosage");
        String[] frequencies = values(req, "itemFrequency");
        String[] durations = values(req, "itemDuration");
        String[] instructions = values(req, "itemInstructions");
        String[] quantities = values(req, "itemQuantity");
        for (int i = 0; i < medicineIds.length; i++) {
            String dosage = at(dosages, i);
            String frequency = at(frequencies, i);
            String quantity = at(quantities, i);
            boolean emptyRow = medicineIds[i].isBlank() && dosage.isBlank() && frequency.isBlank() && quantity.isBlank();
            if (emptyRow) {
                continue;
            }
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicineId(parseLong(medicineIds[i]));
            item.setDosage(dosage);
            item.setFrequency(frequency);
            item.setDuration(at(durations, i));
            item.setInstructions(at(instructions, i));
            item.setQuantity(parseInt(quantity));
            p.addItem(item);
        }
        return p;
    }

    private static String[] values(HttpServletRequest req, String name) {
        String[] v = req.getParameterValues(name);
        return v == null ? new String[0] : v;
    }

    private static String at(String[] array, int index) {
        return index < array.length && array[index] != null ? array[index].trim() : "";
    }
}
