<c:set var="pageTitle" value="Doctors"/><c:set var="nav" value="doctors"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Doctors</h1><p class="lead-text">Prescribing doctors and the prescriptions linked to them.</p></div>
    <c:if test="${perm.DOCTOR_MANAGE}"><div class="page-actions"><a class="btn btn-primary" href="${ctx}/doctors/new"><ui:icon name="plus-lg"/> Add doctor</a></div></c:if>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/doctors" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search doctors</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by ID, name, specialization or hospital" value="${fn:escapeXml(param.q)}">
            </div>
            <div class="form-check ms-1 mb-2">
                <input class="form-check-input" type="checkbox" id="inactive" name="inactive" value="1" ${includeInactive ? 'checked' : ''} data-autosubmit>
                <label class="form-check-label" for="inactive">Include deactivated</label>
            </div>
            <button class="btn btn-outline-primary" type="submit">Search</button>
            <c:if test="${not empty param.q or includeInactive}"><a class="btn btn-outline-secondary" href="${ctx}/doctors">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q}"><ui:empty icon="search" title="No doctors match your search"/></c:when>
                <c:otherwise><ui:empty icon="person-badge" title="No doctors added yet" text="A prescription needs a registered doctor."><c:if test="${perm.DOCTOR_MANAGE}"><a class="btn btn-primary" href="${ctx}/doctors/new">Add doctor</a></c:if></ui:empty></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="table">
                    <caption class="visually-hidden">Doctors</caption>
                    <thead><tr>
                        <ui:th key="code" label="Doctor ID"/><ui:th key="name" label="Name"/><ui:th key="specialization" label="Specialization"/>
                        <ui:th key="hospital" label="Hospital / clinic"/><th scope="col">Phone</th><th scope="col"><span class="visually-hidden">Actions</span></th>
                    </tr></thead>
                    <tbody>
                    <c:forEach var="d" items="${page.items}">
                        <tr class="${d.active ? '' : 'row-inactive'}">
                            <td><a class="row-link" href="${ctx}/doctors/view?id=${d.id}"><c:out value="${d.doctorCode}"/></a></td>
                            <td><c:out value="${d.displayName}"/> <c:if test="${not d.active}"><span class="pill pill-muted">Deactivated</span></c:if></td>
                            <td><c:out value="${d.specialization}"/></td>
                            <td><c:out value="${empty d.hospital ? '-' : d.hospital}"/></td>
                            <td><c:out value="${d.phone}"/></td>
                            <td class="actions">
                                <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/doctors/view?id=${d.id}" aria-label="View ${fn:escapeXml(d.fullName)}"><ui:icon name="eye"/></a>
                                <c:if test="${perm.DOCTOR_MANAGE}"><a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/doctors/edit?id=${d.id}" aria-label="Edit ${fn:escapeXml(d.fullName)}"><ui:icon name="pencil"/></a></c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <ui:pager data="${page}" noun="doctors"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
