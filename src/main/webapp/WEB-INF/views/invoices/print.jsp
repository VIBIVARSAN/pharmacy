<c:set var="pageTitle" value="Print ${invoice.invoiceNo}"/><c:set var="nav" value="invoices"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="print-bar">
    <a class="btn btn-outline-secondary" href="${ctx}/invoices/view?id=${invoice.id}"><ui:icon name="arrow-left"/> Back to invoice</a>
    <div class="d-flex align-items-center gap-3">
        <span class="text-muted small">To save a PDF, choose "Save as PDF" as the printer.</span>
        <button type="button" class="btn btn-primary" data-print><ui:icon name="printer"/> Print / Save as PDF</button>
    </div>
</div>
<article class="doc">
    <header class="doc-head">
        <div>
            <h1><c:out value="${settings.pharmacy_name}"/></h1>
            <c:if test="${not empty settings.pharmacy_address}"><div><c:out value="${settings.pharmacy_address}"/></div></c:if>
            <c:if test="${not empty settings.pharmacy_phone}"><div>Phone: <c:out value="${settings.pharmacy_phone}"/></div></c:if>
            <c:if test="${not empty settings.pharmacy_license}"><div>Licence No. <c:out value="${settings.pharmacy_license}"/></div></c:if>
        </div>
        <div>
            <div class="doc-title">Invoice</div>
            <div class="text-end"><strong><c:out value="${invoice.invoiceNo}"/></strong></div>
            <div class="text-end">${f:dateTime(invoice.createdAt)}</div>
            <c:if test="${invoice.cancelled}"><div class="text-end mt-2"><span class="watermark">Cancelled</span></div></c:if>
        </div>
    </header>
    <section class="doc-meta">
        <div>
            <h2>Billed to</h2>
            <strong><c:out value="${invoice.patientName}"/></strong> (<c:out value="${invoice.patientCode}"/>)<br>
            <c:if test="${not empty invoice.patientAddress}"><c:out value="${invoice.patientAddress}"/><br></c:if>
            <c:out value="${invoice.patientPhone}"/>
        </div>
        <div>
            <h2>Reference</h2>
            Prescription: <c:out value="${invoice.prescriptionNo}"/><br>
            Dispensing: <c:out value="${invoice.dispensingNo}"/><br>
            Payment status: <strong><c:out value="${invoice.cancelled ? 'Cancelled' : invoice.paymentStatus.label}"/></strong>
        </div>
    </section>
    <table>
        <thead><tr><th scope="col">#</th><th scope="col">Medicine</th><th scope="col">Batch</th><th scope="col" class="num">Qty</th><th scope="col" class="num">Unit price</th><th scope="col" class="num">Discount</th><th scope="col" class="num">Amount (<c:out value="${cur}"/>)</th></tr></thead>
        <tbody>
        <c:forEach var="line" items="${invoice.items}" varStatus="row">
            <tr>
                <td>${row.count}</td>
                <td><c:out value="${line.medicineName}"/></td>
                <td><c:out value="${line.batchNumber}"/></td>
                <td class="num">${line.quantity}</td>
                <td class="num">${f:money(line.unitPrice)}</td>
                <td class="num">${f:money(line.discountAmount)}</td>
                <td class="num">${f:money(line.lineTotal)}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div class="totals-box mt-3">
        <div><span>Subtotal</span><span><c:out value="${cur}"/> ${f:money(invoice.subtotal)}</span></div>
        <div><span>Discount</span><span>- <c:out value="${cur}"/> ${f:money(invoice.discountAmount)}</span></div>
        <div><span>Tax (${invoice.taxRate}%)</span><span><c:out value="${cur}"/> ${f:money(invoice.taxAmount)}</span></div>
        <div class="grand"><span>Grand total</span><span><c:out value="${cur}"/> ${f:money(invoice.grandTotal)}</span></div>
        <div><span>Paid</span><span><c:out value="${cur}"/> ${f:money(invoice.amountPaid)}</span></div>
        <c:if test="${not invoice.cancelled}"><div><strong>Balance due</strong><strong><c:out value="${cur}"/> ${f:money(invoice.balanceDue)}</strong></div></c:if>
    </div>
    <c:if test="${not empty invoice.payments}">
        <p class="mt-3 mb-1"><strong>Payments received</strong></p>
        <table>
            <thead><tr><th scope="col">Date</th><th scope="col">Method</th><th scope="col">Reference</th><th scope="col" class="num">Amount (<c:out value="${cur}"/>)</th></tr></thead>
            <tbody>
            <c:forEach var="p" items="${invoice.payments}">
                <tr><td>${f:dateTime(p.paidAt)}</td><td><c:out value="${p.method}"/></td><td><c:out value="${empty p.reference ? '-' : p.reference}"/></td><td class="num">${f:money(p.amount)}</td></tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
    <footer class="doc-foot">
        <div>Prepared by <c:out value="${invoice.createdByName}"/><br>Thank you.</div>
        <div class="signature">Authorised signature</div>
    </footer>
</article>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
