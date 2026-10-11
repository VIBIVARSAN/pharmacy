<c:set var="pageTitle" value="Prescription ${prescription.prescriptionNo}"/><c:set var="nav" value="prescriptions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<c:set var="untouched" value="${empty dispensings}"/>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/prescriptions">Prescriptions</a> / <c:out value="${prescription.prescriptionNo}"/></div>
        <h1><c:out value="${prescription.prescriptionNo}"/> <ui:status value="${prescription.status}"/></h1>
    </div>
    <div class="page-actions">
        <c:if test="${prescription.status.dispensable and perm.DISPENSE}"><a class="btn btn-primary" href="${ctx}/dispensing/new?prescriptionId=${prescription.id}"><ui:icon name="bag-check"/> Dispense</a></c:if>
        <c:if test="${perm.PRESCRIPTION_MANAGE and prescription.status == 'DRAFT'}">
            <form method="post" action="${ctx}/prescriptions/issue" data-confirm="Issue this prescription? Once issued it becomes available for dispensing." data-confirm-label="Issue prescription">
                <ui:csrf/><input type="hidden" name="id" value="${prescription.id}">
                <button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> Issue</button>
            </form>
        </c:if>
        <c:if test="${perm.PRESCRIPTION_MANAGE and prescription.status.editable and untouched}"><a class="btn btn-outline-secondary" href="${ctx}/prescriptions/edit?id=${prescription.id}"><ui:icon name="pencil"/> Edit</a></c:if>
        <c:if test="${prescription.status != 'DRAFT' and prescription.status != 'CANCELLED'}"><a class="btn btn-outline-secondary" href="${ctx}/prescriptions/print?id=${prescription.id}"><ui:icon name="printer"/> Print</a></c:if>
        <c:if test="${perm.PRESCRIPTION_MANAGE and prescription.status.cancellable and untouched}">
            <button type="button" class="btn btn-outline-danger" data-bs-toggle="modal" data-bs-target="#cancelModal"><ui:icon name="x-circle"/> Cancel prescription</button>
        </c:if>
    </div>
</div>

<c:if test="${prescription.status == 'DRAFT'}"><div class="alert alert-warning">This prescription is a draft. It cannot be dispensed until it is issued.</div></c:if>

<div class="card mb-4">
    <div class="card-header"><h2>Prescription details</h2></div>
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Patient</dt><dd><a href="${ctx}/patients/view?id=${prescription.patient.id}"><c:out value="${prescription.patient.fullName}"/></a><br><span class="text-muted small"><c:out value="${prescription.patient.patientCode}"/><c:if test="${not empty prescription.patient.age}"> &middot; ${prescription.patient.age} yrs</c:if> &middot; <c:out value="${prescription.patient.gender.label}"/></span></dd></div>
            <div><dt>Doctor</dt><dd><a href="${ctx}/doctors/view?id=${prescription.doctor.id}"><c:out value="${prescription.doctor.displayName}"/></a><br><span class="text-muted small"><c:out value="${prescription.doctor.specialization}"/></span></dd></div>
            <div><dt>Prescription date</dt><dd>${f:date(prescription.prescriptionDate)}</dd></div>
            <div><dt>Entered by</dt><dd><c:out value="${prescription.createdByName}"/><br><span class="text-muted small">${f:dateTime(prescription.createdAt)}</span></dd></div>
            <div class="wide"><dt>Diagnosis</dt><dd><c:out value="${empty prescription.diagnosis ? 'Not recorded' : prescription.diagnosis}"/></dd></div>
            <div class="wide"><dt>Notes</dt><dd><c:out value="${empty prescription.notes ? 'None' : prescription.notes}"/></dd></div>
        </dl>
    </div>
</div>

<div class="card mb-4">
    <div class="card-header"><h2>Prescribed medicines</h2></div>
    <div class="table-wrap"><table class="table">
        <thead><tr><th scope="col">Medicine</th><th scope="col">Dosage</th><th scope="col">Frequency</th><th scope="col">Duration</th><th scope="col">Instructions</th>
            <th scope="col" class="text-end">Prescribed</th><th scope="col" class="text-end">Dispensed</th><th scope="col" class="text-end">Remaining</th></tr></thead>
        <tbody>
        <c:forEach var="item" items="${prescription.items}">
            <tr>
                <td><a class="row-link" href="${ctx}/medicines/view?id=${item.medicineId}"><c:out value="${item.medicineName}"/></a></td>
                <td><c:out value="${item.dosage}"/></td>
                <td><c:out value="${item.frequency}"/></td>
                <td><c:out value="${empty item.duration ? '-' : item.duration}"/></td>
                <td><c:out value="${empty item.instructions ? '-' : item.instructions}"/></td>
                <td class="text-end">${item.quantity}</td>
                <td class="text-end">${item.dispensedQuantity}</td>
                <td class="text-end"><strong>${item.remainingQuantity}</strong></td>
            </tr>
        </c:forEach>
        </tbody>
    </table></div>
</div>

<div class="card">
    <div class="card-header"><h2>Dispensing history</h2></div>
    <c:choose>
        <c:when test="${empty dispensings}"><ui:empty icon="bag-check" title="Nothing dispensed yet" text="Each hand-over of medicines against this prescription will be listed here."/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <thead><tr><th scope="col">Dispensing</th><th scope="col">Date</th><th scope="col">Dispensed by</th><th scope="col" class="text-end">Value (<c:out value="${cur}"/>)</th><th scope="col">Invoice</th></tr></thead>
                <tbody>
                <c:forEach var="d" items="${dispensings}">
                    <tr>
                        <td><c:choose><c:when test="${perm.DISPENSE}"><a class="row-link" href="${ctx}/dispensing/view?id=${d.id}"><c:out value="${d.dispensingNo}"/></a></c:when><c:otherwise><c:out value="${d.dispensingNo}"/></c:otherwise></c:choose></td>
                        <td>${f:dateTime(d.dispensedAt)}</td>
                        <td><c:out value="${d.dispensedByName}"/></td>
                        <td class="text-end">${f:money(d.total)}</td>
                        <td><c:choose><c:when test="${d.billed}"><a href="${ctx}/invoices/view?id=${d.invoiceId}"><c:out value="${d.invoiceNo}"/></a></c:when><c:otherwise><span class="pill pill-warn">Not billed</span></c:otherwise></c:choose></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${perm.PRESCRIPTION_MANAGE and prescription.status.cancellable and untouched}">
<div class="modal fade" id="cancelModal" tabindex="-1" aria-labelledby="cancelTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <form class="modal-content needs-validation" method="post" action="${ctx}/prescriptions/cancel" novalidate data-once>
            <ui:csrf/><input type="hidden" name="id" value="${prescription.id}">
            <input type="hidden" name="returnTo" value="/prescriptions/view?id=${prescription.id}">
            <div class="modal-header"><h2 class="modal-title h5" id="cancelTitle">Cancel prescription</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
            <div class="modal-body">
                <p>The prescription stays on record with status "Cancelled" and can no longer be dispensed. This cannot be undone.</p>
                <label class="form-label" for="cancelReason">Reason <span class="req" aria-hidden="true">*</span></label>
                <input class="form-control" id="cancelReason" name="reason" type="text" maxlength="200" required data-autofocus>
                <div class="invalid-feedback">Enter the reason for cancelling.</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Keep prescription</button>
                <button type="submit" class="btn btn-danger">Cancel prescription</button>
            </div>
        </form>
    </div>
</div>
</c:if>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
