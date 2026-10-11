package com.pharmacy.servlet;

import com.pharmacy.model.User;
import com.pharmacy.util.PageRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Global search box in the top bar: looks in patients, doctors, medicines, prescriptions and invoices. */
@WebServlet("/search")
public class SearchServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        String q = str(req, "q");
        req.setAttribute("q", q);
        req.setAttribute("qEncoded", q == null ? "" : java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8));
        if (q != null && q.length() >= 2 && q.length() <= 100) {
            PageRequest top = new PageRequest(1, 8, null, false, q);
            req.setAttribute("patients", app().getPatientService().search(user, top, false));
            req.setAttribute("doctors", app().getDoctorService().search(user, top, false));
            req.setAttribute("medicines", app().getMedicineService().search(user, top, null, null, false));
            req.setAttribute("prescriptions", app().getPrescriptionService().search(user, top, null, null, null, null, null));
            req.setAttribute("invoices", app().getBillingService().search(user, top, null, null, null, null, null));
            req.setAttribute("searched", true);
        }
        render(req, resp, "search");
    }
}
