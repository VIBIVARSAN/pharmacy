<c:set var="pageTitle" value="Dispensing"/><c:set var="nav" value="dispensing"/><c:set var="dispTab" value="queue"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Dispensing</h1><p class="lead-text">Issued prescriptions that still have medicines to hand out.</p></div>
</div>
<%@ include file="/WEB-INF/views/dispensing/tabs.jspf" %>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/dispensing" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Find a prescription</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Find by prescription number, patient or doctor" value="${fn:escapeXml(param.q)}">
            </div>
            <button class="btn btn-outline-primary" type="submit">Search</button>
            <c:if test="${not empty param.q}"><a class="btn btn-outline-secondary" href="${ctx}/dispensing">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q}"><ui:empty icon="search" title="No eligible prescription matches your search" text="Only issued or partially dispensed prescriptions appear here."/></c:when>
                <c:otherwise><ui:empty icon="bag-check" title="Nothing is waiting to be dispensed" text="Prescriptions appear here as soon as they are issued."/></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Prescriptions awaiting dispensing</caption>
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
                        <td class="actions"><a class="btn btn-sm btn-primary" href="${ctx}/dispensing/new?prescriptionId=${rx.id}"><ui:icon name="bag-check"/> Dispense</a></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="prescriptions"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
