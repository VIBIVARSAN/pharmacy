<c:set var="pageTitle" value="${supplier.name}"/><c:set var="nav" value="suppliers"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/suppliers">Suppliers</a> / <c:out value="${supplier.supplierCode}"/></div>
        <h1><c:out value="${supplier.name}"/> <c:if test="${not supplier.active}"><span class="pill pill-muted">Deactivated</span></c:if></h1>
    </div>
    <c:if test="${perm.SUPPLIER_MANAGE}">
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/suppliers/edit?id=${supplier.id}"><ui:icon name="pencil"/> Edit</a>
        <c:choose>
            <c:when test="${supplier.active}">
                <form method="post" action="${ctx}/suppliers/deactivate" data-confirm="Deactivate this supplier? Past stock receipts keep their reference, but new stock cannot be received from this supplier." data-confirm-label="Deactivate" data-confirm-style="danger">
                    <ui:csrf/><input type="hidden" name="id" value="${supplier.id}">
                    <button class="btn btn-outline-danger" type="submit"><ui:icon name="slash-circle"/> Deactivate</button>
                </form>
            </c:when>
            <c:otherwise>
                <form method="post" action="${ctx}/suppliers/reactivate">
                    <ui:csrf/><input type="hidden" name="id" value="${supplier.id}">
                    <button class="btn btn-outline-primary" type="submit"><ui:icon name="arrow-repeat"/> Reactivate</button>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
    </c:if>
</div>
<div class="card mb-4">
    <div class="card-header"><h2>Supplier details</h2></div>
    <div class="card-body">
        <dl class="detail-grid">
            <div><dt>Supplier ID</dt><dd><c:out value="${supplier.supplierCode}"/></dd></div>
            <div><dt>Contact person</dt><dd><c:out value="${empty supplier.contactPerson ? 'Not recorded' : supplier.contactPerson}"/></dd></div>
            <div><dt>Phone</dt><dd><c:out value="${supplier.phone}"/></dd></div>
            <div><dt>Email</dt><dd><c:out value="${empty supplier.email ? 'Not recorded' : supplier.email}"/></dd></div>
            <div class="wide"><dt>Address</dt><dd><c:out value="${empty supplier.address ? 'Not recorded' : supplier.address}"/></dd></div>
        </dl>
    </div>
</div>
<div class="card">
    <div class="card-header"><h2>Batches supplied</h2><span class="text-muted small">${batches.total} total</span></div>
    <c:choose>
        <c:when test="${empty batches.items}"><ui:empty icon="box-seam" title="No batches received" text="Stock received from this supplier will be listed here."/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <thead><tr><th scope="col">Medicine</th><th scope="col">Batch</th><th scope="col">Received</th><th scope="col">Expiry</th><th scope="col" class="text-end">On hand</th></tr></thead>
                <tbody>
                <c:forEach var="b" items="${batches.items}">
                    <tr>
                        <td><a class="row-link" href="${ctx}/medicines/view?id=${b.medicineId}"><c:out value="${b.medicineName}"/></a></td>
                        <td><c:out value="${b.batchNumber}"/></td>
                        <td>${f:date(b.receivedDate)}</td>
                        <td>${f:date(b.expiryDate)} <c:if test="${b.expired}"><ui:status value="EXPIRED"/></c:if></td>
                        <td class="text-end">${b.quantity}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table></div>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
