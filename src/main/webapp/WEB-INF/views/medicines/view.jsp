<c:set var="pageTitle" value="${medicine.label}"/><c:set var="nav" value="medicines"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/medicines">Medicines</a> / <c:out value="${medicine.medicineCode}"/></div>
        <h1><c:out value="${medicine.label}"/> <c:if test="${not medicine.active}"><span class="pill pill-muted">Deactivated</span></c:if></h1>
    </div>
    <div class="page-actions">
        <c:if test="${perm.STOCK_IN and medicine.active}"><a class="btn btn-primary" href="${ctx}/inventory/stock-in?medicineId=${medicine.id}"><ui:icon name="box-arrow-in-down"/> Receive stock</a></c:if>
        <c:if test="${perm.MEDICINE_MANAGE}">
            <a class="btn btn-outline-secondary" href="${ctx}/medicines/edit?id=${medicine.id}"><ui:icon name="pencil"/> Edit</a>
            <c:choose>
                <c:when test="${medicine.active}">
                    <form method="post" action="${ctx}/medicines/deactivate" data-confirm="Deactivate this medicine? It can no longer be prescribed or received, but its batches and history are kept." data-confirm-label="Deactivate" data-confirm-style="danger">
                        <ui:csrf/><input type="hidden" name="id" value="${medicine.id}">
                        <button class="btn btn-outline-danger" type="submit"><ui:icon name="slash-circle"/> Deactivate</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/medicines/reactivate">
                        <ui:csrf/><input type="hidden" name="id" value="${medicine.id}">
                        <button class="btn btn-outline-primary" type="submit"><ui:icon name="arrow-repeat"/> Reactivate</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>

<c:if test="${medicine.active and medicine.lowStock}">
    <div class="alert alert-warning d-flex gap-2" role="status"><ui:icon name="exclamation-triangle" css="mt-1"/><div><strong>Low stock.</strong> Usable stock (${medicine.availableStock}) is at or below the reorder level (${medicine.reorderLevel}).</div></div>
</c:if>
<c:if test="${medicine.expiredStock > 0}">
    <div class="alert alert-danger d-flex gap-2" role="status"><ui:icon name="calendar-x" css="mt-1"/><div><strong>${medicine.expiredStock} expired unit(s) on hand.</strong> Expired batches cannot be dispensed. Remove them with "Remove stock" once disposed.</div></div>
</c:if>

<div class="row g-3 mb-4">
    <div class="col-sm-4"><div class="stat-card"><span class="stat-icon"><ui:icon name="box-seam"/></span><span><span class="stat-label">Usable stock</span><br><span class="stat-value">${medicine.availableStock} <small><c:out value="${medicine.unit}"/></small></span></span></div></div>
    <div class="col-sm-4"><div class="stat-card"><span class="stat-icon navy"><ui:icon name="sliders"/></span><span><span class="stat-label">Reorder level</span><br><span class="stat-value">${medicine.reorderLevel}</span></span></div></div>
    <div class="col-sm-4"><div class="stat-card"><span class="stat-icon ${medicine.expiredStock > 0 ? 'bad' : ''}"><ui:icon name="calendar-x"/></span><span><span class="stat-label">Expired stock</span><br><span class="stat-value">${medicine.expiredStock}</span></span></div></div>
</div>

<div class="card mb-4">
    <div class="card-header"><h2>Catalog details</h2></div>
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Medicine ID</dt><dd><c:out value="${medicine.medicineCode}"/></dd></div>
            <div><dt>Generic name</dt><dd><c:out value="${empty medicine.genericName ? 'Not recorded' : medicine.genericName}"/></dd></div>
            <div><dt>Category</dt><dd><c:out value="${medicine.category}"/></dd></div>
            <div><dt>Dosage form</dt><dd><c:out value="${medicine.dosageForm}"/></dd></div>
            <div><dt>Strength</dt><dd><c:out value="${empty medicine.strength ? 'Not recorded' : medicine.strength}"/></dd></div>
            <div><dt>Unit</dt><dd><c:out value="${medicine.unit}"/></dd></div>
            <div><dt>Manufacturer</dt><dd><c:out value="${empty medicine.manufacturer ? 'Not recorded' : medicine.manufacturer}"/></dd></div>
            <div class="wide"><dt>Description</dt><dd><c:out value="${empty medicine.description ? 'None' : medicine.description}"/></dd></div>
        </dl>
    </div>
</div>

<div class="card mb-4">
    <div class="card-header"><h2>Batches</h2><span class="text-muted small">Dispensing uses the earliest-expiring valid batch first (FEFO)</span></div>
    <c:choose>
        <c:when test="${empty batches}"><ui:empty icon="box-seam" title="No batches yet" text="Receive stock to create the first batch of this medicine."/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <thead><tr>
                    <th scope="col">Batch</th><th scope="col">Expiry</th><th scope="col">Supplier</th><th scope="col">Received</th>
                    <th scope="col" class="text-end">Cost (<c:out value="${cur}"/>)</th><th scope="col" class="text-end">Price (<c:out value="${cur}"/>)</th>
                    <th scope="col" class="text-end">On hand</th><th scope="col"><span class="visually-hidden">Actions</span></th>
                </tr></thead>
                <tbody>
                <c:forEach var="b" items="${batches}">
                    <c:set var="near" value="${b.isNearExpiry(nearExpiryDays)}"/>
                    <tr class="${b.expired and b.quantity > 0 ? 'row-bad' : (near and b.quantity > 0 ? 'row-warn' : (b.quantity == 0 ? 'row-inactive' : ''))}">
                        <td><strong><c:out value="${b.batchNumber}"/></strong></td>
                        <td>${f:date(b.expiryDate)}
                            <c:choose>
                                <c:when test="${b.expired}"><ui:status value="EXPIRED"/></c:when>
                                <c:when test="${near}"><span class="pill pill-warn">${b.daysToExpiry} days left</span></c:when>
                            </c:choose>
                        </td>
                        <td><c:out value="${empty b.supplierName ? '-' : b.supplierName}"/></td>
                        <td>${f:date(b.receivedDate)}</td>
                        <td class="text-end">${f:money(b.purchasePrice)}</td>
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
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <div class="card-header"><h2>Recent stock movements</h2><a class="small" href="${ctx}/inventory/transactions?medicineId=${medicine.id}">View all</a></div>
    <c:choose>
        <c:when test="${empty transactions}"><ui:empty icon="clock-history" title="No stock movements yet"/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <thead><tr><th scope="col">Date</th><th scope="col">Type</th><th scope="col">Batch</th><th scope="col" class="text-end">Quantity</th><th scope="col" class="text-end">Batch balance</th><th scope="col">Reason</th><th scope="col">User</th></tr></thead>
                <tbody>
                <c:forEach var="t" items="${transactions}">
                    <tr>
                        <td>${f:dateTime(t.createdAt)}</td>
                        <td><span class="pill ${t.type.inbound ? 'pill-ok' : 'pill-muted'}"><c:out value="${t.type.label}"/></span></td>
                        <td><c:out value="${t.batchNumber}"/></td>
                        <td class="text-end">${t.type.inbound ? '+' : '-'}${t.quantity}</td>
                        <td class="text-end">${t.balanceAfter}</td>
                        <td><c:out value="${t.reason}"/></td>
                        <td><c:out value="${t.username}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
        </c:otherwise>
    </c:choose>
</div>
<c:set var="stockReturnTo" value="/medicines/view?id=${medicine.id}"/>
<%@ include file="/WEB-INF/views/layout/stock-modals.jspf" %>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
