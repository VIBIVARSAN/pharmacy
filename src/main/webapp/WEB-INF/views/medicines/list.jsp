<c:set var="pageTitle" value="Medicines"/><c:set var="nav" value="medicines"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Medicines</h1><p class="lead-text">The medicine catalog with usable stock calculated from all non-expired batches.</p></div>
    <div class="page-actions">
        <c:if test="${perm.STOCK_IN}"><a class="btn btn-outline-secondary" href="${ctx}/inventory/stock-in"><ui:icon name="box-arrow-in-down"/> Receive stock</a></c:if>
        <c:if test="${perm.MEDICINE_MANAGE}"><a class="btn btn-primary" href="${ctx}/medicines/new"><ui:icon name="plus-lg"/> Add medicine</a></c:if>
    </div>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/medicines" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search medicines</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by ID, name, generic name or manufacturer" value="${fn:escapeXml(param.q)}">
            </div>
            <div>
                <label for="category">Category</label>
                <select class="form-select" id="category" name="category" data-autosubmit>
                    <option value="">All categories</option>
                    <c:forEach var="cat" items="${categories}"><option value="${fn:escapeXml(cat)}" ${param.category == cat ? 'selected' : ''}><c:out value="${cat}"/></option></c:forEach>
                </select>
            </div>
            <div>
                <label for="stock">Stock</label>
                <select class="form-select" id="stock" name="stock" data-autosubmit>
                    <option value="">Any stock level</option>
                    <option value="low" ${param.stock == 'low' ? 'selected' : ''}>Low stock</option>
                    <option value="out" ${param.stock == 'out' ? 'selected' : ''}>Out of stock</option>
                </select>
            </div>
            <div class="form-check ms-1 mb-2">
                <input class="form-check-input" type="checkbox" id="inactive" name="inactive" value="1" ${includeInactive ? 'checked' : ''} data-autosubmit>
                <label class="form-check-label" for="inactive">Include deactivated</label>
            </div>
            <button class="btn btn-outline-primary" type="submit">Search</button>
            <c:if test="${not empty param.q or not empty param.category or not empty param.stock or includeInactive}"><a class="btn btn-outline-secondary" href="${ctx}/medicines">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q or not empty param.category or not empty param.stock}"><ui:empty icon="search" title="No medicines match these filters"/></c:when>
                <c:otherwise><ui:empty icon="capsule" title="The catalog is empty" text="Add medicines first, then receive stock for them."><c:if test="${perm.MEDICINE_MANAGE}"><a class="btn btn-primary" href="${ctx}/medicines/new">Add medicine</a></c:if></ui:empty></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="table">
                    <caption class="visually-hidden">Medicines</caption>
                    <thead><tr>
                        <ui:th key="code" label="Medicine ID"/><ui:th key="name" label="Medicine"/><ui:th key="category" label="Category"/>
                        <ui:th key="stock" label="Usable stock" numeric="true"/><ui:th key="reorder" label="Reorder level" numeric="true"/>
                        <th scope="col">Status</th><th scope="col"><span class="visually-hidden">Actions</span></th>
                    </tr></thead>
                    <tbody>
                    <c:forEach var="m" items="${page.items}">
                        <tr class="${not m.active ? 'row-inactive' : (m.availableStock == 0 ? 'row-bad' : (m.lowStock ? 'row-warn' : ''))}">
                            <td><a class="row-link" href="${ctx}/medicines/view?id=${m.id}"><c:out value="${m.medicineCode}"/></a></td>
                            <td><c:out value="${m.label}"/><c:if test="${not empty m.genericName}"><br><span class="text-muted small"><c:out value="${m.genericName}"/></span></c:if></td>
                            <td><c:out value="${m.category}"/></td>
                            <td class="text-end">${m.availableStock} <span class="text-muted small"><c:out value="${m.unit}"/></span></td>
                            <td class="text-end">${m.reorderLevel}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${not m.active}"><span class="pill pill-muted">Deactivated</span></c:when>
                                    <c:when test="${m.availableStock == 0}"><ui:status value="OUT_OF_STOCK"/></c:when>
                                    <c:when test="${m.lowStock}"><ui:status value="LOW_STOCK"/></c:when>
                                    <c:otherwise><span class="pill pill-ok">In stock</span></c:otherwise>
                                </c:choose>
                                <c:if test="${m.expiredStock > 0}"> <span class="pill pill-bad">${m.expiredStock} expired</span></c:if>
                            </td>
                            <td class="actions">
                                <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/medicines/view?id=${m.id}" aria-label="View ${fn:escapeXml(m.name)}"><ui:icon name="eye"/></a>
                                <c:if test="${perm.MEDICINE_MANAGE}"><a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/medicines/edit?id=${m.id}" aria-label="Edit ${fn:escapeXml(m.name)}"><ui:icon name="pencil"/></a></c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <ui:pager data="${page}" noun="medicines"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
