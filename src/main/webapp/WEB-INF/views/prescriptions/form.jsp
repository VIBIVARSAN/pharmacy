<c:set var="isNew" value="${empty prescription.id}"/>
<c:set var="pageTitle" value="${isNew ? 'New prescription' : 'Edit prescription'}"/><c:set var="nav" value="prescriptions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/prescriptions">Prescriptions</a> / <c:out value="${isNew ? 'New' : prescription.prescriptionNo}"/></div>
        <h1><c:out value="${pageTitle}"/></h1>
    </div>
</div>
<div class="alert alert-info d-flex gap-2"><ui:icon name="info-circle" css="mt-1 flex-shrink-0"/><div>Enter the prescription exactly as written by the doctor. The system does not suggest diagnoses, medicines or dosages.</div></div>
<ui:errors/>
<form method="post" action="${ctx}/prescriptions/save" class="needs-validation" novalidate data-once>
    <ui:csrf/>
    <input type="hidden" name="id" value="${prescription.id}">
    <div class="card mb-4">
        <div class="card-header"><h2>Patient and doctor</h2></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-5">
                    <label class="form-label" for="patientText">Patient <span class="req" aria-hidden="true">*</span></label>
                    <div class="lookup" data-lookup="patients">
                        <input class="form-control ${not empty errors.patientId ? 'is-invalid' : ''}" id="patientText" type="text" name="patientLabel" data-lookup-text required
                               placeholder="Type a name, patient ID or phone" value="${fn:escapeXml(prescription.patient.fullName)}">
                        <input type="hidden" name="patientId" data-lookup-id value="${prescription.patient.id}">
                    </div>
                    <c:choose>
                        <c:when test="${not empty errors.patientId}"><div class="invalid-feedback d-block"><c:out value="${errors.patientId}"/></div></c:when>
                        <c:otherwise><div class="form-text">Only registered, active patients can be selected. <a href="${ctx}/patients/new">Register a new patient</a></div></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4">
                    <label class="form-label" for="doctorText">Doctor <span class="req" aria-hidden="true">*</span></label>
                    <div class="lookup" data-lookup="doctors">
                        <input class="form-control ${not empty errors.doctorId ? 'is-invalid' : ''}" id="doctorText" type="text" name="doctorLabel" data-lookup-text required
                               placeholder="Type a name or doctor ID" value="${fn:escapeXml(empty prescription.doctor.doctorCode ? prescription.doctor.fullName : prescription.doctor.displayName)}">
                        <input type="hidden" name="doctorId" data-lookup-id value="${prescription.doctor.id}">
                    </div>
                    <c:if test="${not empty errors.doctorId}"><div class="invalid-feedback d-block"><c:out value="${errors.doctorId}"/></div></c:if>
                </div>
                <ui:input name="prescriptionDate" label="Prescription date" type="date" value="${prescription.prescriptionDate}" required="true" max="${today}" col="col-md-3"/>
                <ui:input name="diagnosis" label="Diagnosis (as stated by the doctor)" type="textarea" value="${prescription.diagnosis}" maxlength="500" rows="2"/>
                <ui:input name="notes" label="Notes" type="textarea" value="${prescription.notes}" maxlength="1000" rows="2"/>
            </div>
        </div>
    </div>

    <div class="card mb-4">
        <div class="card-header"><h2>Prescribed medicines</h2><button type="button" class="btn btn-sm btn-outline-primary" id="addItemRow"><ui:icon name="plus-lg"/> Add medicine</button></div>
        <div class="card-body">
            <c:if test="${not empty errors.items}"><div class="alert alert-danger"><c:out value="${errors.items}"/></div></c:if>
            <c:if test="${empty medicines}"><div class="alert alert-warning">There are no active medicines in the catalog yet. Add medicines before creating a prescription.</div></c:if>
            <div id="itemRows" ${empty prescription.items ? 'data-start-with-row' : ''}>
                <c:forEach var="item" items="${prescription.items}">
                    <%@ include file="/WEB-INF/views/prescriptions/item-row.jspf" %>
                </c:forEach>
            </div>
            <p class="text-muted mb-0" id="itemRowsEmpty" hidden>No medicines added. Use "Add medicine" to add one.</p>
        </div>
    </div>

    <div class="form-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/prescriptions${isNew ? '' : '/view?id='}${isNew ? '' : prescription.id}">Cancel</a>
        <c:choose>
            <c:when test="${isNew or prescription.status == 'DRAFT'}">
                <button class="btn btn-outline-primary" type="submit" name="action" value="draft">Save as draft</button>
                <button class="btn btn-primary" type="submit" name="action" value="issue" data-confirm="Issue this prescription? Once issued it becomes available for dispensing." data-confirm-label="Issue prescription"><ui:icon name="check-lg"/> Save and issue</button>
            </c:when>
            <c:otherwise>
                <button class="btn btn-primary" type="submit" name="action" value="save"><ui:icon name="check-lg"/> Save changes</button>
            </c:otherwise>
        </c:choose>
    </div>
</form>
<template id="itemRowTemplate">
    <c:remove var="item"/>
    <%@ include file="/WEB-INF/views/prescriptions/item-row.jspf" %>
</template>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
