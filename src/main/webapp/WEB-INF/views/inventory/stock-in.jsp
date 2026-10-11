<c:set var="pageTitle" value="Receive stock"/><c:set var="nav" value="inventory"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/inventory">Inventory</a> / Receive stock</div>
        <h1>Receive stock</h1>
        <p class="lead-text">Record a delivery. A new batch number creates a batch; an existing batch number of the same medicine is topped up.</p>
    </div>
</div>
<c:choose>
<c:when test="${empty medicines}">
    <div class="card"><ui:empty icon="capsule" title="No active medicines in the catalog" text="Add a medicine before receiving stock."><c:if test="${perm.MEDICINE_MANAGE}"><a class="btn btn-primary" href="${ctx}/medicines/new">Add medicine</a></c:if></ui:empty></div>
</c:when>
<c:otherwise>
<div class="card">
    <div class="card-body">
        <ui:errors/>
        <form method="post" action="${ctx}/inventory/stock-in" class="needs-validation row g-3" novalidate data-once>
            <ui:csrf/>
            <div class="col-md-6">
                <label class="form-label" for="f_medicineId">Medicine <span class="req" aria-hidden="true">*</span></label>
                <select class="form-select ${not empty errors.medicineId ? 'is-invalid' : ''}" id="f_medicineId" name="medicineId" required>
                    <option value="">Select a medicine...</option>
                    <c:forEach var="m" items="${medicines}"><option value="${m.id}" ${batch.medicineId == m.id ? 'selected' : ''}><c:out value="${m.label}"/> (<c:out value="${m.medicineCode}"/>)</option></c:forEach>
                </select>
                <c:if test="${not empty errors.medicineId}"><div class="invalid-feedback d-block"><c:out value="${errors.medicineId}"/></div></c:if>
            </div>
            <div class="col-md-6">
                <label class="form-label" for="f_supplierId">Supplier</label>
                <select class="form-select ${not empty errors.supplierId ? 'is-invalid' : ''}" id="f_supplierId" name="supplierId">
                    <option value="">Not recorded</option>
                    <c:forEach var="s" items="${suppliers}"><option value="${s.id}" ${batch.supplierId == s.id ? 'selected' : ''}><c:out value="${s.name}"/> (<c:out value="${s.supplierCode}"/>)</option></c:forEach>
                </select>
                <c:if test="${not empty errors.supplierId}"><div class="invalid-feedback d-block"><c:out value="${errors.supplierId}"/></div></c:if>
            </div>
            <ui:input name="batchNumber" label="Batch number" value="${batch.batchNumber}" required="true" maxlength="50" col="col-md-4" help="As printed on the pack."/>
            <ui:input name="expiryDate" label="Expiry date" type="date" value="${batch.expiryDate}" required="true" min="${today}" col="col-md-4" help="Expired stock cannot be received."/>
            <ui:input name="receivedDate" label="Received date" type="date" value="${batch.receivedDate}" required="true" max="${today}" col="col-md-4"/>
            <ui:input name="quantity" label="Quantity received" type="number" value="${quantityValue}" required="true" min="1" step="1" col="col-md-4"/>
            <ui:input name="purchasePrice" label="Purchase price per unit (${cur})" type="number" value="${empty purchasePriceValue ? batch.purchasePrice : purchasePriceValue}" required="true" min="0" step="0.01" col="col-md-4"/>
            <ui:input name="sellingPrice" label="Selling price per unit (${cur})" type="number" value="${empty sellingPriceValue ? batch.sellingPrice : sellingPriceValue}" required="true" min="0" step="0.01" col="col-md-4" help="Used when this batch is dispensed and billed."/>
            <div class="col-12 form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/inventory">Cancel</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> Receive stock</button>
            </div>
        </form>
    </div>
</div>
</c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
