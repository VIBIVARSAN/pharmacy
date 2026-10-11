package com.pharmacy.servlet;

import com.pharmacy.model.PaymentStatus;
import com.pharmacy.model.PrescriptionStatus;
import com.pharmacy.model.ReportResult;
import com.pharmacy.model.StockTransactionType;
import com.pharmacy.model.Supplier;
import com.pharmacy.model.User;
import com.pharmacy.service.ReportService;
import com.pharmacy.util.PageRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** All reports share one JSP; this servlet decides which filters each report offers. */
@WebServlet("/reports/*")
public class ReportServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (path.isEmpty()) {
            render(req, resp, "reports/index");
            return;
        }
        User user = currentUser(req);
        ReportService service = app().getReportService();
        PageRequest pr = pageRequest(req);
        if (req.getParameter("size") == null) {
            pr = new PageRequest(pr.getPage(), 25, null, false, null);
            req.setAttribute("pageRequest", pr);
        }

        // Date-based reports default to the last 30 days on first load.
        LocalDate from = dateParam(req, "from");
        LocalDate to = dateParam(req, "to");
        if (req.getParameter("from") == null && req.getParameter("to") == null) {
            to = LocalDate.now();
            from = to.minusDays(29);
        }
        String filter = str(req, "filter");
        Map<String, String> options = new LinkedHashMap<>();
        ReportResult result;
        String title;
        String description;
        String filterLabel = null;
        boolean dates = true;

        switch (path) {
            case "/sales" -> {
                title = "Sales report";
                description = "Invoiced sales (cancelled invoices excluded), grouped by day, week or month.";
                filterLabel = "Group by";
                options.put("day", "Day");
                options.put("week", "Week");
                options.put("month", "Month");
                if (filter == null || !options.containsKey(filter)) {
                    filter = "day";
                }
                result = service.sales(user, from, to, filter, pr);
            }
            case "/prescriptions" -> {
                title = "Prescription history";
                description = "Prescriptions by prescription date. Clinical details are not included.";
                filterLabel = "Status";
                options.put("", "All statuses");
                for (PrescriptionStatus s : PrescriptionStatus.values()) {
                    options.put(s.name(), s.getLabel());
                }
                filter = options.containsKey(filter) ? filter : "";
                result = service.prescriptions(user, from, to, filter, pr);
            }
            case "/inventory" -> {
                title = "Medicine inventory";
                description = "Current stock of every active medicine, valued at purchase cost.";
                dates = false;
                result = service.inventory(user, pr);
            }
            case "/low-stock" -> {
                title = "Low stock";
                description = "Active medicines whose usable stock is at or below their reorder level.";
                dates = false;
                result = service.lowStock(user, pr);
            }
            case "/expiry" -> {
                title = "Expired and near-expiry batches";
                description = "Batches with stock on hand that have expired or will expire within "
                        + app().getSettingsService().getNearExpiryDays() + " days.";
                dates = false;
                filterLabel = "Show";
                options.put("", "Expired and near expiry");
                options.put("expired", "Expired only");
                options.put("near", "Near expiry only");
                filter = options.containsKey(filter) ? filter : "";
                result = service.expiry(user, filter, pr);
            }
            case "/stock-movements" -> {
                title = "Stock movements";
                description = "Every entry in the stock ledger for the selected period.";
                filterLabel = "Type";
                options.put("", "All types");
                for (StockTransactionType t : StockTransactionType.values()) {
                    options.put(t.name(), t.getLabel());
                }
                filter = options.containsKey(filter) ? filter : "";
                result = service.stockMovements(user, from, to, filter, pr);
            }
            case "/supplier-receipts" -> {
                title = "Supplier stock receipts";
                description = "Stock received from suppliers in the selected period.";
                filterLabel = "Supplier";
                options.put("", "All suppliers");
                for (Supplier s : app().getSupplierService().search(user, PageRequest.firstPage(100), true).getItems()) {
                    options.put(String.valueOf(s.getId()), s.getName());
                }
                filter = options.containsKey(filter) ? filter : "";
                result = service.supplierReceipts(user, from, to, parseLong(filter), pr);
            }
            case "/billing" -> {
                title = "Billing and payment status";
                description = "Invoices with the amounts billed, received and still outstanding.";
                filterLabel = "Payment status";
                options.put("", "All invoices");
                for (PaymentStatus s : PaymentStatus.values()) {
                    options.put(s.name(), s.getLabel());
                }
                options.put("CANCELLED", "Cancelled");
                filter = options.containsKey(filter) ? filter : "";
                result = service.billing(user, from, to, filter, pr);
            }
            default -> {
                super.get(path, req, resp);
                return;
            }
        }
        req.setAttribute("reportPath", path);
        req.setAttribute("reportTitle", title);
        req.setAttribute("reportDescription", description);
        req.setAttribute("showDates", dates);
        req.setAttribute("from", from);
        req.setAttribute("to", to);
        req.setAttribute("filterLabel", filterLabel);
        req.setAttribute("filterOptions", options);
        req.setAttribute("filter", filter);
        req.setAttribute("result", result);
        req.setAttribute("generatedAt", java.time.LocalDateTime.now());
        render(req, resp, "reports/report");
    }
}
