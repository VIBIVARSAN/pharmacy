<c:set var="pageTitle" value="Invoice ${invoice.invoiceNo}"/><c:set var="nav" value="invoices"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<c:set var="canPay" value="${not invoice.cancelled and invoice.paymentStatus != 'PAID' and perm.BILLING}"/>
<c:set var="canCancel" value="${not invoice.cancelled and empty invoice.payments and perm.INVOICE_CANCEL}"/>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/invoices">Billing</a> / <c:out value="${invoice.invoiceNo}"/></div>
        <h1><c:out value="${invoice.invoiceNo}"/>
            <c:choose><c:when test="${invoice.cancelled}"><ui:status value="CANCELLED"/></c:when><c:otherwise><ui:status value="${invoice.paymentStatus}"/></c:otherwise></c:choose></h1>
    </div>
    <div class="page-actions">
        <c:if test="${canPay}"><button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#payModal"><ui:icon name="cash-coin"/> Record payment</button></c:if>
        <a class="btn btn-outline-secondary" href="${ctx}/invoices/print?id=${invoice.id}"><ui:icon name="printer"/> Print</a>
        <c:if test="${canCancel}"><button type="button" class="btn btn-outline-danger" data-bs-toggle="modal" data-bs-target="#cancelModal"><ui:icon name="x-circle"/> Cancel invoice</button></c:if>
    </div>
</div>

<c:if test="${invoice.cancelled}">
    <div class="alert alert-danger"><strong>Cancelled on ${f:dateTime(invoice.cancelledAt)}.</strong> Reason: <c:out value="${invoice.cancelReason}"/>. The invoice is kept for the audit trail and is excluded from sales.</div>
</c:if>

<div class="card mb-4">
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Patient</dt><dd><a href="${ctx}/patients/view?id=${invoice.patientId}"><c:out value="${invoice.patientName}"/></a><br><span class="text-muted small"><c:out value="${invoice.patientCode}"/></span></dd></div>
            <div><dt>Prescription</dt><dd><a href="${ctx}/prescriptions/view?id=${invoice.prescriptionId}"><c:out value="${invoice.prescriptionNo}"/></a></dd></div>
            <div><dt>Dispensing</dt><dd><a href="${ctx}/dispensing/view?id=${invoice.dispensingId}"><c:out value="${invoice.dispensingNo}"/></a></dd></div>
            <div><dt>Invoice date</dt><dd>${f:dateTime(invoice.createdAt)}</dd></div>
            <div><dt>Created by</dt><dd><c:out value="${invoice.createdByName}"/></dd></div>
        </dl>
    </div>
</div>

<div class="card mb-4">
    <div class="card-header"><h2>Line items</h2></div>
    <div class="table-wrap"><table class="table">
        <thead><tr><th scope="col">Medicine</th><th scope="col">Batch</th><th scope="col" class="text-end">Quantity</th><th scope="col" class="text-end">Unit price (<c:out value="${cur}"/>)</th>
            <th scope="col" class="text-end">Discount (<c:out value="${cur}"/>)</th><th scope="col" class="text-end">Line total (<c:out value="${cur}"/>)</th></tr></thead>
        <tbody>
        <c:forEach var="line" items="${invoice.items}">
            <tr>
                <td><c:out value="${line.medicineName}"/></td>
                <td><c:out value="${line.batchNumber}"/></td>
                <td class="text-end">${line.quantity}</td>
                <td class="text-end">${f:money(line.unitPrice)}</td>
                <td class="text-end">${f:money(line.discountAmount)}</td>
                <td class="text-end">${f:money(line.lineTotal)}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table></div>
    <div class="card-body border-top">
        <div class="totals-box">
            <div><span>Subtotal</span><span><c:out value="${cur}"/> ${f:money(invoice.subtotal)}</span></div>
            <div><span>Discount</span><span>- <c:out value="${cur}"/> ${f:money(invoice.discountAmount)}</span></div>
            <div><span>Tax (${invoice.taxRate}%)</span><span><c:out value="${cur}"/> ${f:money(invoice.taxAmount)}</span></div>
            <div class="grand"><span>Grand total</span><span><c:out value="${cur}"/> ${f:money(invoice.grandTotal)}</span></div>
            <div><span>Paid</span><span><c:out value="${cur}"/> ${f:money(invoice.amountPaid)}</span></div>
            <c:if test="${not invoice.cancelled}"><div><strong>Balance due</strong><strong><c:out value="${cur}"/> ${f:money(invoice.balanceDue)}</strong></div></c:if>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-header"><h2>Payments</h2></div>
    <c:choose>
        <c:when test="${empty invoice.payments}"><ui:empty icon="cash-coin" title="No payments recorded" text="${invoice.cancelled ? 'This invoice was cancelled before any payment was taken.' : 'Record a payment when the patient pays.'}"/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <thead><tr><th scope="col">Date</th><th scope="col">Method</th><th scope="col">Reference</th><th scope="col">Received by</th><th scope="col" class="text-end">Amount (<c:out value="${cur}"/>)</th></tr></thead>
                <tbody>
                <c:forEach var="p" items="${invoice.payments}">
                    <tr>
                        <td>${f:dateTime(p.paidAt)}</td>
                        <td><c:out value="${p.method}"/></td>
                        <td><c:out value="${empty p.reference ? '-' : p.reference}"/></td>
                        <td><c:out value="${p.receivedByName}"/></td>
                        <td class="text-end">${f:money(p.amount)}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${canPay}">
<div class="modal fade" id="payModal" tabindex="-1" aria-labelledby="payTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <form class="modal-content needs-validation" method="post" action="${ctx}/invoices/pay" novalidate data-once>
            <ui:csrf/><input type="hidden" name="id" value="${invoice.id}">
            <input type="hidden" name="returnTo" value="/invoices/view?id=${invoice.id}">
            <div class="modal-header"><h2 class="modal-title h5" id="payTitle">Record payment</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
            <div class="modal-body">
                <p>Balance due: <strong><c:out value="${cur}"/> ${f:money(invoice.balanceDue)}</strong></p>
                <div class="mb-3">
                    <label class="form-label" for="pay_amount">Amount (<c:out value="${cur}"/>) <span class="req" aria-hidden="true">*</span></label>
                    <input class="form-control" id="pay_amount" name="paymentAmount" type="number" min="0.01" max="${invoice.balanceDue}" step="0.01" value="${invoice.balanceDue}" required data-autofocus>
                    <div class="invalid-feedback">Enter an amount between 0.01 and the balance due.</div>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="pay_method">Payment method <span class="req" aria-hidden="true">*</span></label>
                    <select class="form-select" id="pay_method" name="paymentMethod" required>
                        <c:forEach var="method" items="${paymentMethods}"><option value="${fn:escapeXml(method)}"><c:out value="${method}"/></option></c:forEach>
                    </select>
                </div>
                <div>
                    <label class="form-label" for="pay_reference">Reference</label>
                    <input class="form-control" id="pay_reference" name="paymentReference" type="text" maxlength="100">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-primary">Record payment</button>
            </div>
        </form>
    </div>
</div>
</c:if>
<c:if test="${canCancel}">
<div class="modal fade" id="cancelModal" tabindex="-1" aria-labelledby="cancelTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <form class="modal-content needs-validation" method="post" action="${ctx}/invoices/cancel" novalidate data-once>
            <ui:csrf/><input type="hidden" name="id" value="${invoice.id}">
            <input type="hidden" name="returnTo" value="/invoices/view?id=${invoice.id}">
            <div class="modal-header"><h2 class="modal-title h5" id="cancelTitle">Cancel invoice</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
            <div class="modal-body">
                <p>The invoice is not deleted: it stays on record as "Cancelled" and the dispensing can be billed again. Stock is not affected. This cannot be undone.</p>
                <label class="form-label" for="cancelReason">Reason <span class="req" aria-hidden="true">*</span></label>
                <input class="form-control" id="cancelReason" name="reason" type="text" maxlength="300" required data-autofocus>
                <div class="invalid-feedback">Enter the reason for cancelling.</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Keep invoice</button>
                <button type="submit" class="btn btn-danger">Cancel invoice</button>
            </div>
        </form>
    </div>
</div>
</c:if>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
