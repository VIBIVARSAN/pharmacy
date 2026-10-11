<c:set var="pageTitle" value="Patients"/><c:set var="nav" value="patients"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Patients</h1><p class="lead-text">Register patients and look up their prescription and billing history.</p></div>
    <div class="page-actions"><a class="btn btn-primary" href="${ctx}/patients/new"><ui:icon name="plus-lg"/> Register patient</a></div>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/patients" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search patients</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by patient ID, name or phone" value="${fn:escapeXml(param.q)}">
            </div>
            <div class="form-check ms-1 mb-2">
                <input class="form-check-input" type="checkbox" id="inactive" name="inactive" value="1" ${includeInactive ? 'checked' : ''} data-autosubmit>
                <label class="form-check-label" for="inactive">Include deactivated</label>
            </div>
            <button class="btn btn-outline-primary" type="submit">Search</button>
            <c:if test="${not empty param.q or includeInactive}"><a class="btn btn-outline-secondary" href="${ctx}/patients">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q}"><ui:empty icon="search" title="No patients match your search" text="Check the spelling or try the patient ID or phone number."/></c:when>
                <c:otherwise><ui:empty icon="people" title="No patients registered yet" text="Register the first patient to start creating prescriptions."><a class="btn btn-primary" href="${ctx}/patients/new">Register patient</a></ui:empty></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="table">
                    <caption class="visually-hidden">Patients</caption>
                    <thead><tr>
                        <ui:th key="code" label="Patient ID"/><ui:th key="name" label="Name"/><th scope="col">Age / gender</th>
                        <ui:th key="phone" label="Phone"/><ui:th key="registered" label="Registered"/><th scope="col"><span class="visually-hidden">Actions</span></th>
                    </tr></thead>
                    <tbody>
                    <c:forEach var="p" items="${page.items}">
                        <tr class="${p.active ? '' : 'row-inactive'}">
                            <td><a class="row-link" href="${ctx}/patients/view?id=${p.id}"><c:out value="${p.patientCode}"/></a></td>
                            <td><c:out value="${p.fullName}"/> <c:if test="${not p.active}"><span class="pill pill-muted">Deactivated</span></c:if></td>
                            <td><c:if test="${not empty p.age}">${p.age} yrs, </c:if><c:out value="${p.gender.label}"/></td>
                            <td><c:out value="${p.phone}"/></td>
                            <td>${f:dateTime(p.createdAt)}</td>
                            <td class="actions">
                                <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/patients/view?id=${p.id}" aria-label="View ${fn:escapeXml(p.fullName)}"><ui:icon name="eye"/></a>
                                <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/patients/edit?id=${p.id}" aria-label="Edit ${fn:escapeXml(p.fullName)}"><ui:icon name="pencil"/></a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <ui:pager data="${page}" noun="patients"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
