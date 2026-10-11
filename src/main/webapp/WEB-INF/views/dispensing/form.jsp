<c:set var="pageTitle" value="Dispense ${prescription.prescriptionNo}"/><c:set var="nav" value="dispensing"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/dispensing">Dispensing</a> / <c:out value="${prescription.prescriptionNo}"/></div>
        <h1>Dispense <c:out value="${prescription.prescriptionNo}"/> <ui:status value="${prescription.status}"/></h1>
    </div>
</div>

<div class="card mb-4">
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Patient</dt><dd><c:out value="${prescription.patient.fullName}"/><br><span class="text-muted small"><c:out value="${prescription.patient.patientCode}"/> &middot; <c:out value="${prescription.patient.phone}"/></span></dd></div>
            <div><dt>Doctor</dt><dd><c:out value="${prescription.doctor.displayName}"/></dd></div>
            <div><dt>Prescription date</dt><dd>${f:date(prescription.prescriptionDate)}</dd></div>
            <c:if test="${not empty prescription.notes}"><div class="wide"><dt>Notes</dt><dd><c:out value="${prescription.notes}"/></dd></div></c:if>
        </dl>
    </div>
</div>

<c:set var="anyPossible" value="${false}"/>
<c:forEach var="item" items="${prescription.items}"><c:if test="${item.remainingQuantity > 0 and item.availableStock > 0}"><c:set var="anyPossible" value="${true}"/></c:if></c:forEach>

<form method="post" action="${ctx}/dispensing/save" class="needs-validation" novalidate data-once
      data-confirm="Confirm dispensing? Stock will be deducted from the earliest-expiring valid batches. This cannot be undone." data-confirm-label="Dispense">
    <ui:csrf/>
    <input type="hidden" name="prescriptionId" value="${prescription.id}">
    <input type="hidden" name="requestToken" value="${requestToken}">
    <input type="hidden" name="returnTo" value="/dispensing/new?prescriptionId=${prescription.id}">
    <div class="card mb-4">
        <div class="card-header">
            <h2>Medicines to hand out</h2>
            <div class="d-flex gap-2">
                <button type="button" class="btn btn-sm btn-outline-secondary" id="fillAllQuantities">Fill maximum</button>
                <button type="button" class="btn btn-sm btn-outline-secondary" id="clearAllQuantities">Clear</button>
            </div>
        </div>
        <div class="table-wrap"><table class="table">
            <caption class="visually-hidden">Prescribed medicines and quantities to dispense</caption>
            <thead><tr><th scope="col">Medicine and directions</th><th scope="col" class="text-end">Prescribed</th><th scope="col" class="text-end">Already dispensed</th>
                <th scope="col" class="text-end">Remaining</th><th scope="col" class="text-end">Usable stock</th><th scope="col" style="width: 170px">Dispense now</th></tr></thead>
            <tbody>
            <c:forEach var="item" items="${prescription.items}">
                <c:set var="maxNow" value="${item.remainingQuantity < item.availableStock ? item.remainingQuantity : item.availableStock}"/>
                <tr class="${item.remainingQuantity > 0 and item.availableStock < item.remainingQuantity ? 'row-warn' : ''}">
                    <td><strong><c:out value="${item.medicineName}"/></strong><br>
                        <span class="text-muted small"><c:out value="${item.dosage}"/> &middot; <c:out value="${item.frequency}"/><c:if test="${not empty item.duration}"> &middot; <c:out value="${item.duration}"/></c:if><c:if test="${not empty item.instructions}"> &middot; <c:out value="${item.instructions}"/></c:if></span></td>
                    <td class="text-end">${item.quantity}</td>
                    <td class="text-end">${item.dispensedQuantity}</td>
                    <td class="text-end"><strong>${item.remainingQuantity}</strong></td>
                    <td class="text-end">${item.availableStock}
                        <c:if test="${item.remainingQuantity > 0 and item.availableStock == 0}"><br><ui:status value="OUT_OF_STOCK"/></c:if>
                        <c:if test="${item.remainingQuantity > 0 and item.availableStock > 0 and item.availableStock < item.remainingQuantity}"><br><span class="pill pill-warn">Partial only</span></c:if>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${item.remainingQuantity == 0}"><span class="pill pill-ok">Complete</span></c:when>
                            <c:when test="${maxNow == 0}"><span class="text-muted">No stock</span></c:when>
                            <c:otherwise>
                                <label class="visually-hidden" for="qty_${item.id}">Quantity of <c:out value="${item.medicineName}"/> to dispense now</label>
                                <input class="form-control text-end" id="qty_${item.id}" name="qty_${item.id}" type="number" min="0" max="${maxNow}" step="1" value="${maxNow}" data-suggested="${maxNow}" required>
                                <div class="invalid-feedback">Enter 0 to ${maxNow}.</div>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table></div>
        <div class="card-body border-top">
            <label class="form-label" for="notes">Notes (optional)</label>
            <input class="form-control" id="notes" name="notes" type="text" maxlength="300" placeholder="e.g. Balance to be collected on Friday">
            <p class="form-text mb-0">Expired batches are never used. If stock is short you can dispense part now; the prescription then stays open as "Partially dispensed".</p>
        </div>
    </div>
    <div class="form-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/dispensing">Cancel</a>
        <c:choose>
            <c:when test="${anyPossible}"><button class="btn btn-primary" type="submit"><ui:icon name="bag-check"/> Dispense medicines</button></c:when>
            <c:otherwise><button class="btn btn-primary" type="button" disabled>No stock available to dispense</button></c:otherwise>
        </c:choose>
    </div>
</form>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
