<c:set var="pageTitle" value="Audit log"/><c:set var="nav" value="audit"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Audit log</h1><p class="lead-text">Who did what and when. Entries cannot be edited or removed from the application.</p></div>
    <div class="page-actions"><button type="button" class="btn btn-outline-secondary" data-print><ui:icon name="printer"/> Print</button></div>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/audit-logs" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search the audit log</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search description or username" value="${fn:escapeXml(param.q)}">
            </div>
            <div>
                <label for="action">Action</label>
                <select class="form-select" id="action" name="action">
                    <option value="">All actions</option>
                    <c:forEach var="a" items="${actions}"><option value="${fn:escapeXml(a)}" ${param.action == a ? 'selected' : ''}><c:out value="${f:label(a)}"/></option></c:forEach>
                </select>
            </div>
            <div>
                <label for="entityType">Record type</label>
                <select class="form-select" id="entityType" name="entityType">
                    <option value="">All types</option>
                    <c:forEach var="t" items="${entityTypes}"><option value="${fn:escapeXml(t)}" ${param.entityType == t ? 'selected' : ''}><c:out value="${f:label(t)}"/></option></c:forEach>
                </select>
            </div>
            <div><label for="from">From</label><input class="form-control" id="from" type="date" name="from" value="${fn:escapeXml(param.from)}"></div>
            <div><label for="to">To</label><input class="form-control" id="to" type="date" name="to" value="${fn:escapeXml(param.to)}"></div>
            <button class="btn btn-outline-primary" type="submit"><ui:icon name="funnel"/> Apply</button>
            <c:if test="${not empty param.q or not empty param.action or not empty param.entityType or not empty param.from or not empty param.to}"><a class="btn btn-outline-secondary" href="${ctx}/audit-logs">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}"><ui:empty icon="journal-text" title="No audit entries match these filters"/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Audit log entries</caption>
                <thead><tr><th scope="col">Date and time</th><th scope="col">User</th><th scope="col">Action</th><th scope="col">Record</th><th scope="col">Description</th><th scope="col">IP address</th></tr></thead>
                <tbody>
                <c:forEach var="a" items="${page.items}">
                    <tr>
                        <td class="text-nowrap">${f:dateTime(a.createdAt)}</td>
                        <td><c:out value="${empty a.username ? '(not signed in)' : a.username}"/></td>
                        <td><span class="pill ${a.action == 'LOGIN_FAILED' or fn:endsWith(a.action, 'CANCELLED') or fn:endsWith(a.action, 'DEACTIVATED') ? 'pill-bad' : 'pill-muted'}"><c:out value="${f:label(a.action)}"/></span></td>
                        <td><c:out value="${f:label(a.entityType)}"/><c:if test="${not empty a.entityId}"> #${a.entityId}</c:if></td>
                        <td><c:out value="${a.description}"/></td>
                        <td><c:out value="${a.ipAddress}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="entries"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
