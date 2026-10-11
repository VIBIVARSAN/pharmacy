<c:set var="pageTitle" value="Users"/><c:set var="nav" value="users"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Users</h1><p class="lead-text">Accounts that can sign in, and what each role is allowed to do.</p></div>
    <div class="page-actions"><a class="btn btn-primary" href="${ctx}/users/new"><ui:icon name="plus-lg"/> Add user</a></div>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/users" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search users</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by username, name or email" value="${fn:escapeXml(param.q)}">
            </div>
            <button class="btn btn-outline-primary" type="submit">Search</button>
            <c:if test="${not empty param.q}"><a class="btn btn-outline-secondary" href="${ctx}/users">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}"><ui:empty icon="search" title="No users match your search"/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Users</caption>
                <thead><tr><ui:th key="username" label="Username"/><ui:th key="name" label="Name"/><ui:th key="role" label="Role"/><th scope="col">Status</th><ui:th key="lastLogin" label="Last sign-in"/><th scope="col"><span class="visually-hidden">Actions</span></th></tr></thead>
                <tbody>
                <c:forEach var="u" items="${page.items}">
                    <tr class="${u.active ? '' : 'row-inactive'}">
                        <td><strong><c:out value="${u.username}"/></strong><c:if test="${u.id == authUser.id}"> <span class="pill pill-info">You</span></c:if></td>
                        <td><c:out value="${u.fullName}"/></td>
                        <td><c:out value="${u.role.label}"/></td>
                        <td><c:choose><c:when test="${u.active}"><ui:status value="ACTIVE"/></c:when><c:otherwise><span class="pill pill-muted">Deactivated</span></c:otherwise></c:choose></td>
                        <td><c:out value="${empty u.lastLoginAt ? 'Never' : f:dateTime(u.lastLoginAt)}"/></td>
                        <td class="actions"><a class="btn btn-sm btn-outline-secondary" href="${ctx}/users/edit?id=${u.id}"><ui:icon name="pencil"/> Edit</a></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="users"/>
        </c:otherwise>
    </c:choose>
</div>
<div class="card mt-4">
    <div class="card-header"><h2>What each role can do</h2></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-6"><strong>Administrator</strong><p class="text-muted mb-0">Everything: users, doctors, patients, medicines, suppliers, prescriptions, inventory (including stock adjustments), billing (including invoice cancellation), reports, audit log and settings.</p></div>
            <div class="col-md-6"><strong>Pharmacist</strong><p class="text-muted mb-0">Register and update patients, view prescriptions, doctors, medicines and suppliers, dispense issued prescriptions, receive and remove stock, create invoices and record payments.</p></div>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
