<c:set var="pageTitle" value="Billing"/><c:set var="nav" value="invoices"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div><h1>Billing</h1><p class="lead-text">Invoices for dispensed medicines and the payments received against them.</p></div>
    <div class="page-actions"><a class="btn btn-primary" href="${ctx}/dispensing/history?billing=unbilled"><ui:icon name="plus-lg"/> Bill a dispensing</a></div>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/invoices" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search invoices</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by invoice, patient or prescription" value="${fn:escapeXml(param.q)}">
            </div>
            <div>
                <label for="filter">Status</label>
                <select class="form-select" id="filter" name="filter">
                    <option value="">All invoices</option>
                    <c:forEach var="s" items="${paymentStatuses}"><option value="${s}" ${param.filter == s.name() ? 'selected' : ''}><c:out value="${s.label}"/></option></c:forEach>
                    <option value="CANCELLED" ${param.filter == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                </select>
            </div>
            <div><label for="from">From</label><input class="form-control" id="from" type="date" name="from" value="${fn:escapeXml(param.from)}"></div>
            <div><label for="to">To</label><input class="form-control" id="to" type="date" name="to" value="${fn:escapeXml(param.to)}"></div>
            <button class="btn btn-outline-primary" type="submit"><ui:icon name="funnel"/> Apply</button>
            <c:if test="${not empty param.q or not empty param.filter or not empty param.from or not empty param.to}"><a class="btn btn-outline-secondary" href="${ctx}/invoices">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q or not empty param.filter or not empty param.from or not empty param.to}"><ui:empty icon="search" title="No invoices match these filters"/></c:when>
                <c:otherwise><ui:empty icon="receipt" title="No invoices yet" text="Invoices are created from dispensing records after medicines are handed out."/></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Invoices</caption>
                <thead><tr>
                    <ui:th key="no" label="Invoice"/><ui:th key="date" label="Date"/><ui:th key="patient" label="Patient"/>
                    <ui:th key="total" label="Total (${cur})" numeric="true"/><th scope="col" class="text-end">Balance (<c:out value="${cur}"/>)</th>
                    <ui:th key="payment" label="Payment"/><th scope="col"><span class="visually-hidden">Actions</span></th>
                </tr></thead>
                <tbody>
                <c:forEach var="inv" items="${page.items}">
                    <tr class="${inv.cancelled ? 'row-inactive' : ''}">
                        <td><a class="row-link" href="${ctx}/invoices/view?id=${inv.id}"><c:out value="${inv.invoiceNo}"/></a></td>
                        <td>${f:dateTime(inv.createdAt)}</td>
                        <td><c:out value="${inv.patientName}"/><br><span class="text-muted small"><c:out value="${inv.patientCode}"/></span></td>
                        <td class="text-end">${f:money(inv.grandTotal)}</td>
                        <td class="text-end"><c:choose><c:when test="${inv.cancelled}">-</c:when><c:otherwise>${f:money(inv.balanceDue)}</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${inv.cancelled}"><ui:status value="CANCELLED"/></c:when><c:otherwise><ui:status value="${inv.paymentStatus}"/></c:otherwise></c:choose></td>
                        <td class="actions">
                            <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/invoices/view?id=${inv.id}" aria-label="View ${fn:escapeXml(inv.invoiceNo)}"><ui:icon name="eye"/></a>
                            <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/invoices/print?id=${inv.id}" aria-label="Print ${fn:escapeXml(inv.invoiceNo)}"><ui:icon name="printer"/></a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="invoices"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
