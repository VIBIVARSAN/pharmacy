<c:set var="pageTitle" value="Dispensing history"/><c:set var="nav" value="dispensing"/><c:set var="dispTab" value="history"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div><h1>Dispensing</h1><p class="lead-text">Every hand-over of medicines, with the batches used and its billing status.</p></div>
</div>
<%@ include file="/WEB-INF/views/dispensing/tabs.jspf" %>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/dispensing/history" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search dispensing records</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by number, prescription or patient" value="${fn:escapeXml(param.q)}">
            </div>
            <div>
                <label for="billing">Billing</label>
                <select class="form-select" id="billing" name="billing">
                    <option value="">All</option>
                    <option value="unbilled" ${param.billing == 'unbilled' ? 'selected' : ''}>Not billed</option>
                    <option value="billed" ${param.billing == 'billed' ? 'selected' : ''}>Billed</option>
                </select>
            </div>
            <div><label for="from">From</label><input class="form-control" id="from" type="date" name="from" value="${fn:escapeXml(param.from)}"></div>
            <div><label for="to">To</label><input class="form-control" id="to" type="date" name="to" value="${fn:escapeXml(param.to)}"></div>
            <button class="btn btn-outline-primary" type="submit"><ui:icon name="funnel"/> Apply</button>
            <c:if test="${not empty param.q or not empty param.billing or not empty param.from or not empty param.to}"><a class="btn btn-outline-secondary" href="${ctx}/dispensing/history">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}"><ui:empty icon="bag-check" title="No dispensing records found" text="Records appear here after medicines are dispensed against a prescription."/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Dispensing records</caption>
                <thead><tr>
                    <ui:th key="no" label="Dispensing"/><ui:th key="date" label="Date"/><ui:th key="prescription" label="Prescription"/><ui:th key="patient" label="Patient"/>
                    <th scope="col" class="text-end">Value (<c:out value="${cur}"/>)</th><th scope="col">Billing</th><th scope="col"><span class="visually-hidden">Actions</span></th>
                </tr></thead>
                <tbody>
                <c:forEach var="d" items="${page.items}">
                    <tr>
                        <td><a class="row-link" href="${ctx}/dispensing/view?id=${d.id}"><c:out value="${d.dispensingNo}"/></a></td>
                        <td>${f:dateTime(d.dispensedAt)}</td>
                        <td><a href="${ctx}/prescriptions/view?id=${d.prescriptionId}"><c:out value="${d.prescriptionNo}"/></a></td>
                        <td><c:out value="${d.patientName}"/><br><span class="text-muted small"><c:out value="${d.patientCode}"/></span></td>
                        <td class="text-end">${f:money(d.total)}</td>
                        <td><c:choose><c:when test="${d.billed}"><a href="${ctx}/invoices/view?id=${d.invoiceId}"><c:out value="${d.invoiceNo}"/></a></c:when><c:otherwise><span class="pill pill-warn">Not billed</span></c:otherwise></c:choose></td>
                        <td class="actions"><c:if test="${not d.billed and perm.BILLING}"><a class="btn btn-sm btn-primary" href="${ctx}/invoices/new?dispensingId=${d.id}"><ui:icon name="receipt"/> Create invoice</a></c:if></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="records"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
