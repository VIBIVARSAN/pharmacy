<c:set var="pageTitle" value="${doctor.displayName}"/><c:set var="nav" value="doctors"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/doctors">Doctors</a> / <c:out value="${doctor.doctorCode}"/></div>
        <h1><c:out value="${doctor.displayName}"/> <c:if test="${not doctor.active}"><span class="pill pill-muted">Deactivated</span></c:if></h1>
    </div>
    <c:if test="${perm.DOCTOR_MANAGE}">
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/doctors/edit?id=${doctor.id}"><ui:icon name="pencil"/> Edit</a>
        <c:choose>
            <c:when test="${doctor.active}">
                <form method="post" action="${ctx}/doctors/deactivate" data-confirm="Deactivate this doctor? Existing prescriptions keep their reference, but no new prescriptions can be created for this doctor." data-confirm-label="Deactivate" data-confirm-style="danger">
                    <ui:csrf/><input type="hidden" name="id" value="${doctor.id}">
                    <button class="btn btn-outline-danger" type="submit"><ui:icon name="slash-circle"/> Deactivate</button>
                </form>
            </c:when>
            <c:otherwise>
                <form method="post" action="${ctx}/doctors/reactivate">
                    <ui:csrf/><input type="hidden" name="id" value="${doctor.id}">
                    <button class="btn btn-outline-primary" type="submit"><ui:icon name="arrow-repeat"/> Reactivate</button>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
    </c:if>
</div>
<div class="card mb-4">
    <div class="card-header"><h2>Doctor details</h2></div>
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Doctor ID</dt><dd><c:out value="${doctor.doctorCode}"/></dd></div>
            <div><dt>Specialization</dt><dd><c:out value="${doctor.specialization}"/></dd></div>
            <div><dt>Registration number</dt><dd><c:out value="${empty doctor.registrationNo ? 'Not recorded' : doctor.registrationNo}"/></dd></div>
            <div><dt>Phone</dt><dd><c:out value="${doctor.phone}"/></dd></div>
            <div><dt>Email</dt><dd><c:out value="${empty doctor.email ? 'Not recorded' : doctor.email}"/></dd></div>
            <div><dt>Added</dt><dd>${f:dateTime(doctor.createdAt)}</dd></div>
            <div class="wide"><dt>Hospital / clinic</dt><dd><c:out value="${empty doctor.hospital ? 'Not recorded' : doctor.hospital}"/></dd></div>
        </dl>
    </div>
</div>
<div class="card">
    <div class="card-header"><h2>Prescriptions by this doctor</h2><span class="text-muted small">${prescriptions.total} total</span></div>
    <c:choose>
        <c:when test="${empty prescriptions.items}"><ui:empty icon="file-medical" title="No prescriptions" text="No prescriptions are linked to this doctor yet."/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <thead><tr><th scope="col">Prescription</th><th scope="col">Date</th><th scope="col">Patient</th><th scope="col">Status</th></tr></thead>
                <tbody>
                <c:forEach var="rx" items="${prescriptions.items}">
                    <tr>
                        <td><a class="row-link" href="${ctx}/prescriptions/view?id=${rx.id}"><c:out value="${rx.prescriptionNo}"/></a></td>
                        <td>${f:date(rx.prescriptionDate)}</td>
                        <td><c:out value="${rx.patient.fullName}"/> (<c:out value="${rx.patient.patientCode}"/>)</td>
                        <td><ui:status value="${rx.status}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
