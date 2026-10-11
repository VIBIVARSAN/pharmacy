package com.pharmacy.servlet;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.MedicineBatch;
import com.pharmacy.model.StockTransactionType;
import com.pharmacy.model.User;
import com.pharmacy.service.InventoryService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;

@WebServlet("/inventory/*")
public class InventoryServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String homePath() {
        return "/inventory";
    }

    @Override
    protected void get(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        InventoryService service = app().getInventoryService();
        switch (path) {
            case "" -> {
                req.setAttribute("page", service.searchBatches(user, pageRequest(req), str(req, "status"), null));
                req.setAttribute("nearExpiryDays", app().getSettingsService().getNearExpiryDays());
                render(req, resp, "inventory/batches");
            }
            case "/stock-in" -> {
                MedicineBatch batch = new MedicineBatch();
                batch.setMedicineId(longParam(req, "medicineId"));
                batch.setReceivedDate(LocalDate.now());
                showStockInForm(req, resp, user, batch);
            }
            case "/transactions" -> {
                req.setAttribute("page", service.searchTransactions(user, pageRequest(req),
                        enumParam(req, "type", StockTransactionType.class), dateParam(req, "from"), dateParam(req, "to"),
                        longParam(req, "medicineId")));
                req.setAttribute("types", StockTransactionType.values());
                render(req, resp, "inventory/transactions");
            }
            default -> super.get(path, req, resp);
        }
    }

    private void showStockInForm(HttpServletRequest req, HttpServletResponse resp, User user, MedicineBatch batch) throws Exception {
        req.setAttribute("batch", batch);
        req.setAttribute("medicines", app().getMedicineService().findAllActive(user));
        req.setAttribute("suppliers", app().getSupplierService().findAllActive(user));
        req.setAttribute("today", LocalDate.now().toString());
        render(req, resp, "inventory/stock-in");
    }

    @Override
    protected void post(String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        InventoryService service = app().getInventoryService();
        switch (path) {
            case "/stock-in" -> {
                MedicineBatch batch = new MedicineBatch();
                batch.setMedicineId(longParam(req, "medicineId"));
                batch.setBatchNumber(str(req, "batchNumber"));
                batch.setExpiryDate(dateParam(req, "expiryDate"));
                batch.setPurchasePrice(decimalParam(req, "purchasePrice"));
                batch.setSellingPrice(decimalParam(req, "sellingPrice"));
                batch.setSupplierId(longParam(req, "supplierId"));
                batch.setReceivedDate(dateParam(req, "receivedDate"));
                try {
                    MedicineBatch saved = service.stockIn(user, batch, intParam(req, "quantity"));
                    flashSuccess(req, "Stock received into batch " + saved.getBatchNumber() + ".");
                    redirect(req, resp, "/medicines/view?id=" + saved.getMedicineId());
                } catch (ValidationException e) {
                    req.setAttribute("errors", e.getErrors());
                    req.setAttribute("quantityValue", req.getParameter("quantity"));
                    req.setAttribute("purchasePriceValue", req.getParameter("purchasePrice"));
                    req.setAttribute("sellingPriceValue", req.getParameter("sellingPrice"));
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    showStockInForm(req, resp, user, batch);
                }
            }
            // The two actions below are submitted from modal dialogs; validation problems are
            // reported by BaseServlet as a flash message on the page the user came from.
            case "/stock-out" -> {
                service.stockOut(user, requiredId(req, "batchId"), intParam(req, "quantity"), str(req, "reason"));
                flashSuccess(req, "Stock removed and recorded in the stock ledger.");
                redirect(req, resp, returnPath(req, "/inventory"));
            }
            case "/adjust" -> {
                boolean increase = "in".equals(req.getParameter("direction"));
                service.adjust(user, requiredId(req, "batchId"), increase, intParam(req, "quantity"), str(req, "reason"));
                flashSuccess(req, "Stock adjustment recorded.");
                redirect(req, resp, returnPath(req, "/inventory"));
            }
            default -> super.post(path, req, resp);
        }
    }
}
