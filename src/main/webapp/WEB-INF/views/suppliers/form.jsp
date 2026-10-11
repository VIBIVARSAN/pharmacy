<c:set var="isNew" value="${empty supplier.id}"/>
<c:set var="pageTitle" value="${isNew ? 'Add supplier' : 'Edit supplier'}"/><c:set var="nav" value="suppliers"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/suppliers">Suppliers</a> / <c:out value="${isNew ? 'New' : supplier.supplierCode}"/></div>
        <h1><c:out value="${pageTitle}"/></h1>
    </div>
</div>
<div class="card">
    <div class="card-body">
        <ui:errors/>
        <form method="post" action="${ctx}/suppliers/save" class="needs-validation row g-3" novalidate data-once>
            <ui:csrf/>
            <input type="hidden" name="id" value="${supplier.id}">
            <ui:input name="name" label="Supplier name" value="${supplier.name}" required="true" maxlength="150"/>
            <ui:input name="contactPerson" label="Contact person" value="${supplier.contactPerson}" maxlength="120"/>
            <ui:input name="phone" label="Phone" type="tel" value="${supplier.phone}" required="true" maxlength="20"/>
            <ui:input name="email" label="Email" type="email" value="${supplier.email}" maxlength="150"/>
            <ui:input name="address" label="Address" type="textarea" value="${supplier.address}" maxlength="300" col="col-12" rows="2"/>
            <div class="col-12 form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/suppliers${isNew ? '' : '/view?id='}${isNew ? '' : supplier.id}">Cancel</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> <c:out value="${isNew ? 'Add supplier' : 'Save changes'}"/></button>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
