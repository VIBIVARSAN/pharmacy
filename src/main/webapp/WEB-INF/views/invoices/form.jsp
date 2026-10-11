<c:set var="pageTitle" value="Create invoice"/><c:set var="nav" value="invoices"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/invoices">Billing</a> / New invoice</div>
        <h1>Create invoice</h1>
        <p class="lead-text">For dispensing <a href="${ctx}/dispensing/view?id=${invoice.dispensingId}"><c:out value="${invoice.dispensingNo}"/></a> &middot; prescription <c:out value="${invoice.prescriptionNo}"/> &middot; <c:out value="${invoice.patientName}"/> (<c:out value="${invoice.patientCode}"/>)</p>
    </div>
</div>
<form method="post" action="${ctx}/invoices/save" class="needs-validation" id="invoiceForm" novalidate data-once data-tax-rate="${invoice.taxRate}"
      data-confirm="Create this invoice? The invoice number is assigned now and the invoice cannot be edited afterwards." data-confirm-label="Create invoice">
    <ui:csrf/>
    <input type="hidden" name="dispensingId" value="${invoice.dispensingId}">
    <input type="hidden" name="returnTo" value="/invoices/new?dispensingId=${invoice.dispensingId}">
    <div class="card mb-4">
        <div class="card-header"><h2>Line items</h2><span class="text-muted small">Stock was already deducted when the medicines were dispensed</span></div>
        <div class="table-wrap"><table class="table">
            <caption class="visually-hidden">Invoice line items</caption>
            <thead><tr><th scope="col">Medicine</th><th scope="col">Batch</th><th scope="col" class="text-end">Quantity</th><th scope="col" class="text-end">Unit price (<c:out value="${cur}"/>)</th>
                <c:if test="${discountsEnabled}"><th scope="col" style="width: 160px">Discount (<c:out value="${cur}"/>)</th></c:if>
                <th scope="col" class="text-end">Line total (<c:out value="${cur}"/>)</th></tr></thead>
            <tbody>
            <c:forEach var="line" items="${invoice.items}">
                <tr data-line data-gross="${line.lineTotal}">
                    <td><strong><c:out value="${line.medicineName}"/></strong></td>
                    <td><c:out value="${line.batchNumber}"/></td>
                    <td class="text-end">${line.quantity}</td>
                    <td class="text-end">${f:money(line.unitPrice)}</td>
                    <c:if test="${discountsEnabled}">
                        <td>
                            <label class="visually-hidden" for="discount_${line.dispensingItemId}">Discount on <c:out value="${line.medicineName}"/></label>
                            <input class="form-control text-end" id="discount_${line.dispensingItemId}" name="discount_${line.dispensingItemId}" type="number" min="0" max="${line.lineTotal}" step="0.01" value="0.00" data-discount>
                            <div class="invalid-feedback">0 to ${f:money(line.lineTotal)}</div>
                        </td>
                    </c:if>
                    <td class="text-end" data-line-total>${f:money(line.lineTotal)}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table></div>
        <div class="card-body border-top">
            <div class="totals-box" aria-live="polite">
                <div><span>Subtotal</span><span><c:out value="${cur}"/> <span id="pvSubtotal">${f:money(invoice.subtotal)}</span></span></div>
                <div><span>Discount</span><span>- <c:out value="${cur}"/> <span id="pvDiscount">${f:money(invoice.discountAmount)}</span></span></div>
                <div><span>Tax (${invoice.taxRate}%)</span><span><c:out value="${cur}"/> <span id="pvTax">${f:money(invoice.taxAmount)}</span></span></div>
                <div class="grand"><span>Grand total</span><span><c:out value="${cur}"/> <span id="pvGrand">${f:money(invoice.grandTotal)}</span></span></div>
            </div>
            <p class="form-text text-end mb-0">Totals are recalculated on the server when the invoice is saved.</p>
        </div>
    </div>

    <div class="card mb-4">
        <div class="card-header"><h2>Payment received now (optional)</h2></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label" for="f_paymentMethod">Payment method</label>
                    <select class="form-select" id="f_paymentMethod" name="paymentMethod">
                        <c:forEach var="method" items="${paymentMethods}"><option value="${fn:escapeXml(method)}"><c:out value="${method}"/></option></c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="f_paymentAmount">Amount (<c:out value="${cur}"/>)</label>
                    <div class="input-group">
                        <input class="form-control" id="f_paymentAmount" name="paymentAmount" type="number" min="0" step="0.01" placeholder="Leave empty if unpaid">
                        <button class="btn btn-outline-secondary" type="button" id="payFull" data-amount="${invoice.grandTotal}">Full amount</button>
                    </div>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="f_paymentReference">Reference</label>
                    <input class="form-control" id="f_paymentReference" name="paymentReference" type="text" maxlength="100" placeholder="Card slip / UPI reference">
                </div>
            </div>
        </div>
    </div>
    <div class="form-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/dispensing/view?id=${invoice.dispensingId}">Cancel</a>
        <button class="btn btn-primary" type="submit"><ui:icon name="receipt"/> Create invoice</button>
    </div>
</form>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
