<c:set var="pageTitle" value="Dispensing ${dispensing.dispensingNo}"/><c:set var="nav" value="dispensing"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/dispensing/history">Dispensing history</a> / <c:out value="${dispensing.dispensingNo}"/></div>
        <h1><c:out value="${dispensing.dispensingNo}"/>
            <c:choose><c:when test="${dispensing.billed}"><span class="pill pill-ok">Billed</span></c:when><c:otherwise><span class="pill pill-warn">Not billed</span></c:otherwise></c:choose></h1>
    </div>
    <div class="page-actions">
        <c:choose>
            <c:when test="${dispensing.billed}"><a class="btn btn-outline-secondary" href="${ctx}/invoices/view?id=${dispensing.invoiceId}"><ui:icon name="receipt"/> View invoice <c:out value="${dispensing.invoiceNo}"/></a></c:when>
            <c:when test="${perm.BILLING}"><a class="btn btn-primary" href="${ctx}/invoices/new?dispensingId=${dispensing.id}"><ui:icon name="receipt"/> Create invoice</a></c:when>
        </c:choose>
    </div>
</div>
<div class="card mb-4">
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Prescription</dt><dd><a href="${ctx}/prescriptions/view?id=${dispensing.prescriptionId}"><c:out value="${dispensing.prescriptionNo}"/></a></dd></div>
            <div><dt>Patient</dt><dd><a href="${ctx}/patients/view?id=${dispensing.patientId}"><c:out value="${dispensing.patientName}"/></a><br><span class="text-muted small"><c:out value="${dispensing.patientCode}"/></span></dd></div>
            <div><dt>Dispensed on</dt><dd>${f:dateTime(dispensing.dispensedAt)}</dd></div>
            <div><dt>Dispensed by</dt><dd><c:out value="${dispensing.dispensedByName}"/></dd></div>
            <c:if test="${not empty dispensing.notes}"><div class="wide"><dt>Notes</dt><dd><c:out value="${dispensing.notes}"/></dd></div></c:if>
        </dl>
    </div>
</div>
<div class="card">
    <div class="card-header"><h2>Items dispensed</h2><span class="text-muted small">One line per batch used</span></div>
    <div class="table-wrap"><table class="table">
        <thead><tr><th scope="col">Medicine</th><th scope="col">Batch</th><th scope="col">Batch expiry</th><th scope="col" class="text-end">Quantity</th>
            <th scope="col" class="text-end">Unit price (<c:out value="${cur}"/>)</th><th scope="col" class="text-end">Amount (<c:out value="${cur}"/>)</th></tr></thead>
        <tbody>
        <c:forEach var="i" items="${dispensing.items}">
            <tr>
                <td><a class="row-link" href="${ctx}/medicines/view?id=${i.medicineId}"><c:out value="${i.medicineName}"/></a></td>
                <td><c:out value="${i.batchNumber}"/></td>
                <td>${f:date(i.expiryDate)}</td>
                <td class="text-end">${i.quantity}</td>
                <td class="text-end">${f:money(i.unitPrice)}</td>
                <td class="text-end">${f:money(i.lineTotal)}</td>
            </tr>
        </c:forEach>
        </tbody>
        <tfoot><tr><td colspan="5" class="text-end">Value before discount and tax</td><td class="text-end">${f:money(dispensing.total)}</td></tr></tfoot>
    </table></div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
