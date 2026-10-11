<c:set var="pageTitle" value="${patient.fullName}"/><c:set var="nav" value="patients"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/patients">Patients</a> / <c:out value="${patient.patientCode}"/></div>
        <h1><c:out value="${patient.fullName}"/> <c:if test="${not patient.active}"><span class="pill pill-muted">Deactivated</span></c:if></h1>
    </div>
    <div class="page-actions">
        <c:if test="${perm.PRESCRIPTION_MANAGE and patient.active}"><a class="btn btn-primary" href="${ctx}/prescriptions/new?patientId=${patient.id}"><ui:icon name="plus-lg"/> New prescription</a></c:if>
        <a class="btn btn-outline-secondary" href="${ctx}/patients/edit?id=${patient.id}"><ui:icon name="pencil"/> Edit</a>
        <c:if test="${perm.PATIENT_DEACTIVATE}">
            <c:choose>
                <c:when test="${patient.active}">
                    <form method="post" action="${ctx}/patients/deactivate" data-confirm="Deactivate this patient? Their history is kept, but new prescriptions cannot be created for them." data-confirm-label="Deactivate" data-confirm-style="danger">
                        <ui:csrf/><input type="hidden" name="id" value="${patient.id}">
                        <button class="btn btn-outline-danger" type="submit"><ui:icon name="slash-circle"/> Deactivate</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/patients/reactivate">
                        <ui:csrf/><input type="hidden" name="id" value="${patient.id}">
                        <button class="btn btn-outline-primary" type="submit"><ui:icon name="arrow-repeat"/> Reactivate</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>

<div class="card mb-4">
    <div class="card-header"><h2>Patient profile</h2></div>
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Patient ID</dt><dd><c:out value="${patient.patientCode}"/></dd></div>
            <div><dt>Gender</dt><dd><c:out value="${patient.gender.label}"/></dd></div>
            <div><dt>Date of birth</dt><dd><c:out value="${empty patient.dateOfBirth ? 'Not recorded' : f:date(patient.dateOfBirth)}"/><c:if test="${not empty patient.age}"> (${patient.age} years)</c:if></dd></div>
            <div><dt>Phone</dt><dd><c:out value="${patient.phone}"/></dd></div>
            <div><dt>Email</dt><dd><c:out value="${empty patient.email ? 'Not recorded' : patient.email}"/></dd></div>
            <div><dt>Registered</dt><dd>${f:dateTime(patient.createdAt)}</dd></div>
            <div class="wide"><dt>Address</dt><dd><c:out value="${empty patient.address ? 'Not recorded' : patient.address}"/></dd></div>
        </dl>
    </div>
</div>

<div class="row g-3">
    <div class="col-xl-6">
        <div class="card h-100">
            <div class="card-header"><h2>Prescription history</h2><span class="text-muted small">${prescriptions.total} total</span></div>
            <c:choose>
                <c:when test="${empty prescriptions.items}"><ui:empty icon="file-medical" title="No prescriptions" text="This patient has no prescriptions on record."/></c:when>
                <c:otherwise>
                    <div class="table-wrap"><table class="table">
                        <thead><tr><th scope="col">Prescription</th><th scope="col">Date</th><th scope="col">Doctor</th><th scope="col">Status</th></tr></thead>
                        <tbody>
                        <c:forEach var="rx" items="${prescriptions.items}">
                            <tr>
                                <td><a class="row-link" href="${ctx}/prescriptions/view?id=${rx.id}"><c:out value="${rx.prescriptionNo}"/></a></td>
                                <td>${f:date(rx.prescriptionDate)}</td>
                                <td><c:out value="${rx.doctor.displayName}"/></td>
                                <td><ui:status value="${rx.status}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table></div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="col-xl-6">
        <div class="card h-100">
            <div class="card-header"><h2>Invoice history</h2><span class="text-muted small">${invoices.total} total</span></div>
            <c:choose>
                <c:when test="${empty invoices.items}"><ui:empty icon="receipt" title="No invoices" text="This patient has not been billed yet."/></c:when>
                <c:otherwise>
                    <div class="table-wrap"><table class="table">
                        <thead><tr><th scope="col">Invoice</th><th scope="col">Date</th><th scope="col" class="text-end">Total (<c:out value="${cur}"/>)</th><th scope="col">Payment</th></tr></thead>
                        <tbody>
                        <c:forEach var="inv" items="${invoices.items}">
                            <tr>
                                <td><a class="row-link" href="${ctx}/invoices/view?id=${inv.id}"><c:out value="${inv.invoiceNo}"/></a></td>
                                <td>${f:dateTime(inv.createdAt)}</td>
                                <td class="text-end">${f:money(inv.grandTotal)}</td>
                                <td><c:choose><c:when test="${inv.cancelled}"><ui:status value="CANCELLED"/></c:when><c:otherwise><ui:status value="${inv.paymentStatus}"/></c:otherwise></c:choose></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table></div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
