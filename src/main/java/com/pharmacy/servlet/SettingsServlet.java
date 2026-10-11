package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Setting;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/settings")
public class SettingsServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> values = new LinkedHashMap<>(app().getSettingsService().asMap());
        show(req, resp, values);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, Map<String, String> values) throws Exception {
        req.setAttribute("settingList", app().getSettingsService().findAll());
        req.setAttribute("values", values);
        render(req, resp, "settings");
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> submitted = new LinkedHashMap<>();
        for (Setting s : app().getSettingsService().findAll()) {
            String value = req.getParameter("s_" + s.getKey());
            if (value != null) {
                submitted.put(s.getKey(), value.trim());
            }
        }
        try {
            app().getSettingsService().update(currentUser(req), submitted);
            flashSuccess(req, "Settings saved.");
            redirect(req, resp, "/settings");
        } catch (ValidationException e) {
            // form fields are named "s_<key>", so re-key the messages to match them
            Map<String, String> errors = new LinkedHashMap<>();
            e.getErrors().forEach((key, message) -> errors.put("s_" + key, message));
            req.setAttribute("errors", errors);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            show(req, resp, submitted);
        }
    }
}
