package com.pharmacy.servlet;

import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.DuplicateOperationException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.InvoiceStatus;
import com.pharmacy.model.PaymentStatus;
import com.pharmacy.model.User;
import com.pharmacy.service.BillingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/invoices/*")
public class InvoiceServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/invoices";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        BillingService service = app().getBillingService();
        switch (path) {
            case "" -> {
                String filter = str(req, "filter");
                InvoiceStatus status = "CANCELLED".equals(filter) ? InvoiceStatus.CANCELLED : null;
                PaymentStatus paymentStatus = status == null ? enumParam(req, "filter", PaymentStatus.class) : null;
                req.setAttribute("page", service.search(user, pageRequest(req), paymentStatus, status,
                        dateParam(req, "from"), dateParam(req, "to"), null));
                req.setAttribute("paymentStatuses", PaymentStatus.values());
                render(req, resp, "invoices/list");
            }
            case "/new" -> {
                long dispensingId = requiredId(req, "dispensingId");
                try {
                    req.setAttribute("invoice", service.preview(user, dispensingId));
                } catch (DuplicateOperationException e) {
                    flashInfo(req, e.getMessage());
                    redirect(req, resp, "/invoices/view?id=" + e.getExistingId());
                    return;
                } catch (BusinessRuleException e) {
                    flashError(req, e.getMessage());
                    redirect(req, resp, "/dispensing/view?id=" + dispensingId);
                    return;
                }
                req.setAttribute("paymentMethods", app().getSettingsService().getPaymentMethods());
                req.setAttribute("discountsEnabled", app().getSettingsService().isDiscountsEnabled());
                render(req, resp, "invoices/form");
            }
            case "/view", "/print" -> {
                Invoice invoice = service.get(user, requiredId(req, "id"));
                req.setAttribute("invoice", invoice);
                req.setAttribute("paymentMethods", app().getSettingsService().getPaymentMethods());
                render(req, resp, path.equals("/print") ? "invoices/print" : "invoices/view");
            }
            default -> super.get(path, req, resp);
        }
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        BillingService service = app().getBillingService();
        switch (path) {
            case "/save" -> {
                long dispensingId = requiredId(req, "dispensingId");
                Map<Long, BigDecimal> discounts = new HashMap<>();
                Enumeration<String> names = req.getParameterNames();
                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    String raw = req.getParameter(name);
                    if (!name.startsWith("discount_") || raw == null || raw.isBlank()) {
                        continue;
                    }
                    Long itemId = parseLong(name.substring(9));
                    BigDecimal amount = parseDecimal(raw);
                    if (itemId == null || amount == null) {
                        throw new ValidationException(name, "Discounts must be amounts with at most 2 decimal places.");
                    }
                    discounts.put(itemId, amount);
                }
                try {
                    Invoice invoice = service.createInvoice(user, dispensingId, discounts, str(req, "paymentMethod"),
                            amount(req, "paymentAmount"), str(req, "paymentReference"));
                    flashSuccess(req, "Invoice " + invoice.getInvoiceNo() + " created.");
                    redirect(req, resp, "/invoices/view?id=" + invoice.getId());
                } catch (DuplicateOperationException e) {
                    flashInfo(req, e.getMessage() + " No second invoice was created.");
                    redirect(req, resp, e.getExistingId() == null ? "/invoices" : "/invoices/view?id=" + e.getExistingId());
                }
            }
            case "/pay" -> {
                long id = requiredId(req, "id");
                BigDecimal paid = amount(req, "paymentAmount");
                if (paid == null) {
                    throw new ValidationException("paymentAmount", "Enter the payment amount.");
                }
                service.addPayment(user, id, paid, str(req, "paymentMethod"), str(req, "paymentReference"));
                flashSuccess(req, "Payment recorded.");
                redirect(req, resp, "/invoices/view?id=" + id);
            }
            case "/cancel" -> {
                long id = requiredId(req, "id");
                service.cancel(user, id, str(req, "reason"));
                flashSuccess(req, "Invoice cancelled. It remains on record for audit purposes.");
                redirect(req, resp, "/invoices/view?id=" + id);
            }
            default -> super.post(path, req, resp);
        }
    }

    /** Blank means "no amount"; anything else must be a valid money value. */
    private static BigDecimal amount(HttpServletRequest req, String name) {
        String raw = req.getParameter(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        BigDecimal value = parseDecimal(raw);
        if (value == null) {
            throw new ValidationException(name, "Enter a valid amount with at most 2 decimal places.");
        }
        return value;
    }
}
