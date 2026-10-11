<c:set var="pageTitle" value="Stock ledger"/><c:set var="nav" value="inventory"/><c:set var="invTab" value="transactions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Stock ledger</h1><p class="lead-text">Every stock movement, in the order it happened. Entries are never edited or deleted.</p></div>
</div>
<%@ include file="/WEB-INF/views/inventory/tabs.jspf" %>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/inventory/transactions" role="search">
            <c:if test="${not empty param.medicineId}"><input type="hidden" name="medicineId" value="${fn:escapeXml(param.medicineId)}"></c:if>
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search movements</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by medicine, batch or reason" value="${fn:escapeXml(param.q)}">
            </div>
            <div>
                <label for="type">Type</label>
                <select class="form-select" id="type" name="type">
                    <option value="">All types</option>
                    <c:forEach var="t" items="${types}"><option value="${t}" ${param.type == t.name() ? 'selected' : ''}><c:out value="${t.label}"/></option></c:forEach>
                </select>
            </div>
            <div><label for="from">From</label><input class="form-control" id="from" type="date" name="from" value="${fn:escapeXml(param.from)}"></div>
            <div><label for="to">To</label><input class="form-control" id="to" type="date" name="to" value="${fn:escapeXml(param.to)}"></div>
            <button class="btn btn-outline-primary" type="submit"><ui:icon name="funnel"/> Apply</button>
            <c:if test="${not empty param.q or not empty param.type or not empty param.from or not empty param.to or not empty param.medicineId}"><a class="btn btn-outline-secondary" href="${ctx}/inventory/transactions">Clear</a></c:if>
        </form>
    </div>
    <c:if test="${not empty param.medicineId}"><div class="px-3 pt-3"><div class="alert alert-info mb-0">Showing movements for one medicine only. <a href="${ctx}/inventory/transactions">Show all medicines</a></div></div></c:if>
    <c:choose>
        <c:when test="${empty page.items}"><ui:empty icon="clock-history" title="No stock movements found" text="Movements appear here when stock is received, removed, adjusted or dispensed."/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Stock movements</caption>
                <thead><tr>
                    <ui:th key="date" label="Date"/><ui:th key="medicine" label="Medicine"/><th scope="col">Batch</th><ui:th key="type" label="Type"/>
                    <ui:th key="quantity" label="Quantity" numeric="true"/><th scope="col" class="text-end">Batch balance</th><th scope="col">Reason</th><th scope="col">User</th>
                </tr></thead>
                <tbody>
                <c:forEach var="t" items="${page.items}">
                    <tr>
                        <td>${f:dateTime(t.createdAt)}</td>
                        <td><a class="row-link" href="${ctx}/medicines/view?id=${t.medicineId}"><c:out value="${t.medicineName}"/></a></td>
                        <td><c:out value="${t.batchNumber}"/></td>
                        <td><span class="pill ${t.type.inbound ? 'pill-ok' : 'pill-muted'}"><c:out value="${t.type.label}"/></span></td>
                        <td class="text-end">${t.type.inbound ? '+' : '-'}${t.quantity}</td>
                        <td class="text-end">${t.balanceAfter}</td>
                        <td><c:out value="${t.reason}"/>
                            <c:if test="${t.referenceType == 'DISPENSING' and perm.DISPENSE}"> <a class="small" href="${ctx}/dispensing/view?id=${t.referenceId}">view</a></c:if></td>
                        <td><c:out value="${t.username}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="movements"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
