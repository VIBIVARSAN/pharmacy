<c:set var="pageTitle" value="Prescriptions"/><c:set var="nav" value="prescriptions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Prescriptions</h1><p class="lead-text">Prescriptions entered from the doctor's order, with their dispensing status.</p></div>
    <c:if test="${perm.PRESCRIPTION_MANAGE}"><div class="page-actions"><a class="btn btn-primary" href="${ctx}/prescriptions/new"><ui:icon name="plus-lg"/> New prescription</a></div></c:if>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/prescriptions" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search prescriptions</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by number, patient or doctor" value="${fn:escapeXml(param.q)}">
            </div>
            <div>
                <label for="status">Status</label>
                <select class="form-select" id="status" name="status">
                    <option value="">All statuses</option>
                    <c:forEach var="s" items="${statuses}"><option value="${s}" ${param.status == s.name() ? 'selected' : ''}><c:out value="${s.label}"/></option></c:forEach>
                </select>
            </div>
            <div><label for="from">From</label><input class="form-control" id="from" type="date" name="from" value="${fn:escapeXml(param.from)}"></div>
            <div><label for="to">To</label><input class="form-control" id="to" type="date" name="to" value="${fn:escapeXml(param.to)}"></div>
            <button class="btn btn-outline-primary" type="submit"><ui:icon name="funnel"/> Apply</button>
            <c:if test="${not empty param.q or not empty param.status or not empty param.from or not empty param.to}"><a class="btn btn-outline-secondary" href="${ctx}/prescriptions">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q or not empty param.status or not empty param.from or not empty param.to}"><ui:empty icon="search" title="No prescriptions match these filters"/></c:when>
                <c:otherwise><ui:empty icon="file-medical" title="No prescriptions yet" text="A prescription links a registered patient and doctor with the prescribed medicines."><c:if test="${perm.PRESCRIPTION_MANAGE}"><a class="btn btn-primary" href="${ctx}/prescriptions/new">New prescription</a></c:if></ui:empty></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Prescriptions</caption>
                <thead><tr>
                    <ui:th key="no" label="Prescription"/><ui:th key="date" label="Date"/><ui:th key="patient" label="Patient"/>
                    <ui:th key="doctor" label="Doctor"/><ui:th key="status" label="Status"/><th scope="col"><span class="visually-hidden">Actions</span></th>
                </tr></thead>
                <tbody>
                <c:forEach var="rx" items="${page.items}">
                    <tr>
                        <td><a class="row-link" href="${ctx}/prescriptions/view?id=${rx.id}"><c:out value="${rx.prescriptionNo}"/></a></td>
                        <td>${f:date(rx.prescriptionDate)}</td>
                        <td><c:out value="${rx.patient.fullName}"/><br><span class="text-muted small"><c:out value="${rx.patient.patientCode}"/></span></td>
                        <td><c:out value="${rx.doctor.displayName}"/></td>
                        <td><ui:status value="${rx.status}"/></td>
                        <td class="actions">
                            <c:if test="${rx.status.dispensable and perm.DISPENSE}"><a class="btn btn-sm btn-primary" href="${ctx}/dispensing/new?prescriptionId=${rx.id}">Dispense</a></c:if>
                            <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/prescriptions/view?id=${rx.id}" aria-label="View ${fn:escapeXml(rx.prescriptionNo)}"><ui:icon name="eye"/></a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="prescriptions"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
