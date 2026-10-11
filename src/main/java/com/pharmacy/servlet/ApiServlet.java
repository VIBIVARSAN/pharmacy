package com.pharmacy.servlet;

import com.pharmacy.exception.AppException;
import com.pharmacy.model.Doctor;
import com.pharmacy.model.Patient;
import com.pharmacy.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small read-only JSON endpoints used by the page scripts (charts,
 * notification bell, patient / doctor autocomplete). The browser never talks
 * to MySQL; it only receives these already-authorized results.
 */
@WebServlet("/api/*")
public class ApiServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        try {
            switch (path) {
                case "/dashboard/sales-trend" -> {
                    Integer days = intParam(req, "days");
                    json(resp, 200, app().getDashboardService().getSalesTrend(days == null ? 7 : days));
                }
                case "/dashboard/stock-by-category" -> json(resp, 200, app().getDashboardService().getStockByCategory());
                case "/notifications" -> json(resp, 200, app().getDashboardService().getAlerts());
                case "/lookup/patients" -> {
                    List<Map<String, Object>> out = new ArrayList<>();
                    for (Patient p : app().getPatientService().lookup(user, str(req, "q"))) {
                        out.add(item(p.getId(), p.getFullName(), p.getPatientCode() + " - " + p.getPhone()));
                    }
                    json(resp, 200, out);
                }
                case "/lookup/doctors" -> {
                    List<Map<String, Object>> out = new ArrayList<>();
                    for (Doctor d : app().getDoctorService().lookup(user, str(req, "q"))) {
                        out.add(item(d.getId(), d.getDisplayName(), d.getDoctorCode() + " - " + d.getSpecialization()));
                    }
                    json(resp, 200, out);
                }
                default -> json(resp, 404, Map.of("error", "Unknown endpoint."));
            }
        } catch (AppException e) {
            int status = e.getHttpStatus();
            json(resp, status, Map.of("error", status >= 500 ? "The server could not complete the request." : e.getMessage()));
        }
    }

    private static Map<String, Object> item(Long id, String label, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("detail", detail);
        return m;
    }
}
