<c:set var="pageTitle" value="Inventory"/><c:set var="nav" value="inventory"/><c:set var="invTab" value="batches"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div><h1>Inventory</h1><p class="lead-text">Every batch on record with its expiry date and quantity on hand.</p></div>
    <c:if test="${perm.STOCK_IN}"><div class="page-actions"><a class="btn btn-primary" href="${ctx}/inventory/stock-in"><ui:icon name="box-arrow-in-down"/> Receive stock</a></div></c:if>
</div>
<%@ include file="/WEB-INF/views/inventory/tabs.jspf" %>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/inventory" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search batches</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by medicine or batch number" value="${fn:escapeXml(param.q)}">
            </div>
            <div>
                <label for="status">Show</label>
                <select class="form-select" id="status" name="status" data-autosubmit>
                    <option value="">All batches</option>
                    <option value="instock" ${param.status == 'instock' ? 'selected' : ''}>Usable stock</option>
                    <option value="near" ${param.status == 'near' ? 'selected' : ''}>Near expiry (${nearExpiryDays} days)</option>
                    <option value="expired" ${param.status == 'expired' ? 'selected' : ''}>Expired with stock</option>
                    <option value="empty" ${param.status == 'empty' ? 'selected' : ''}>Empty batches</option>
                </select>
            </div>
            <button class="btn btn-outline-primary" type="submit">Search</button>
            <c:if test="${not empty param.q or not empty param.status}"><a class="btn btn-outline-secondary" href="${ctx}/inventory">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q or not empty param.status}"><ui:empty icon="search" title="No batches match these filters"/></c:when>
                <c:otherwise><ui:empty icon="box-seam" title="No stock has been received yet" text="Receive stock to create batches."><c:if test="${perm.STOCK_IN}"><a class="btn btn-primary" href="${ctx}/inventory/stock-in">Receive stock</a></c:if></ui:empty></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden">Medicine batches</caption>
                <thead><tr>
                    <ui:th key="medicine" label="Medicine"/><ui:th key="batch" label="Batch"/><ui:th key="expiry" label="Expiry"/>
                    <th scope="col">Supplier</th><th scope="col" class="text-end">Price (<c:out value="${cur}"/>)</th>
                    <ui:th key="quantity" label="On hand" numeric="true"/><th scope="col"><span class="visually-hidden">Actions</span></th>
                </tr></thead>
                <tbody>
                <c:forEach var="b" items="${page.items}">
                    <c:set var="near" value="${b.isNearExpiry(nearExpiryDays)}"/>
                    <tr class="${b.expired and b.quantity > 0 ? 'row-bad' : (near and b.quantity > 0 ? 'row-warn' : (b.quantity == 0 ? 'row-inactive' : ''))}">
                        <td><a class="row-link" href="${ctx}/medicines/view?id=${b.medicineId}"><c:out value="${b.medicineName}"/></a><br><span class="text-muted small"><c:out value="${b.medicineCode}"/></span></td>
                        <td><c:out value="${b.batchNumber}"/></td>
                        <td>${f:date(b.expiryDate)}
                            <c:choose>
                                <c:when test="${b.expired}"><ui:status value="EXPIRED"/></c:when>
                                <c:when test="${near}"><span class="pill pill-warn">${b.daysToExpiry} days left</span></c:when>
                            </c:choose>
                        </td>
                        <td><c:out value="${empty b.supplierName ? '-' : b.supplierName}"/></td>
                        <td class="text-end">${f:money(b.sellingPrice)}</td>
                        <td class="text-end"><strong>${b.quantity}</strong></td>
                        <td class="actions">
                            <c:if test="${perm.STOCK_OUT and b.quantity > 0}">
                                <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#stockOutModal"
                                        data-fill-batch-id="${b.id}" data-fill-label="${fn:escapeXml(b.batchNumber)}" data-fill-quantity="${b.quantity}"><ui:icon name="box-arrow-up"/> Remove</button>
                            </c:if>
                            <c:if test="${perm.STOCK_ADJUST}">
                                <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#adjustModal"
                                        data-fill-batch-id="${b.id}" data-fill-label="${fn:escapeXml(b.batchNumber)}" data-fill-quantity="${b.quantity}"><ui:icon name="sliders"/> Adjust</button>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="batches"/>
        </c:otherwise>
    </c:choose>
</div>
<c:set var="stockReturnTo" value="/inventory${empty param.status ? '' : '?status='}${fn:escapeXml(param.status)}"/>
<%@ include file="/WEB-INF/views/layout/stock-modals.jspf" %>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
