<c:set var="pageTitle" value="Suppliers"/><c:set var="nav" value="suppliers"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Suppliers</h1><p class="lead-text">Companies that supply medicine batches to the pharmacy.</p></div>
    <c:if test="${perm.SUPPLIER_MANAGE}"><div class="page-actions"><a class="btn btn-primary" href="${ctx}/suppliers/new"><ui:icon name="plus-lg"/> Add supplier</a></div></c:if>
</div>
<div class="card">
    <div class="card-header">
        <form class="toolbar w-100" method="get" action="${ctx}/suppliers" role="search">
            <div class="search-box">
                <ui:icon name="search"/>
                <label class="visually-hidden" for="q">Search suppliers</label>
                <input class="form-control" id="q" type="search" name="q" maxlength="100" placeholder="Search by ID, name, contact or phone" value="${fn:escapeXml(param.q)}">
            </div>
            <div class="form-check ms-1 mb-2">
                <input class="form-check-input" type="checkbox" id="inactive" name="inactive" value="1" ${includeInactive ? 'checked' : ''} data-autosubmit>
                <label class="form-check-label" for="inactive">Include deactivated</label>
            </div>
            <button class="btn btn-outline-primary" type="submit">Search</button>
            <c:if test="${not empty param.q or includeInactive}"><a class="btn btn-outline-secondary" href="${ctx}/suppliers">Clear</a></c:if>
        </form>
    </div>
    <c:choose>
        <c:when test="${empty page.items}">
            <c:choose>
                <c:when test="${not empty param.q}"><ui:empty icon="search" title="No suppliers match your search"/></c:when>
                <c:otherwise><ui:empty icon="truck" title="No suppliers added yet" text="Add suppliers so stock receipts can be traced back to them."><c:if test="${perm.SUPPLIER_MANAGE}"><a class="btn btn-primary" href="${ctx}/suppliers/new">Add supplier</a></c:if></ui:empty></c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table class="table">
                    <caption class="visually-hidden">Suppliers</caption>
                    <thead><tr>
                        <ui:th key="code" label="Supplier ID"/><ui:th key="name" label="Name"/><ui:th key="contact" label="Contact person"/>
                        <th scope="col">Phone</th><th scope="col">Email</th><th scope="col"><span class="visually-hidden">Actions</span></th>
                    </tr></thead>
                    <tbody>
                    <c:forEach var="s" items="${page.items}">
                        <tr class="${s.active ? '' : 'row-inactive'}">
                            <td><a class="row-link" href="${ctx}/suppliers/view?id=${s.id}"><c:out value="${s.supplierCode}"/></a></td>
                            <td><c:out value="${s.name}"/> <c:if test="${not s.active}"><span class="pill pill-muted">Deactivated</span></c:if></td>
                            <td><c:out value="${empty s.contactPerson ? '-' : s.contactPerson}"/></td>
                            <td><c:out value="${s.phone}"/></td>
                            <td><c:out value="${empty s.email ? '-' : s.email}"/></td>
                            <td class="actions">
                                <a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/suppliers/view?id=${s.id}" aria-label="View ${fn:escapeXml(s.name)}"><ui:icon name="eye"/></a>
                                <c:if test="${perm.SUPPLIER_MANAGE}"><a class="btn btn-sm btn-outline-secondary btn-icon" href="${ctx}/suppliers/edit?id=${s.id}" aria-label="Edit ${fn:escapeXml(s.name)}"><ui:icon name="pencil"/></a></c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <ui:pager data="${page}" noun="suppliers"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
