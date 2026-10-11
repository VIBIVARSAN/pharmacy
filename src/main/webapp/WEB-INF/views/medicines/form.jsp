<c:set var="isNew" value="${empty medicine.id}"/>
<c:set var="pageTitle" value="${isNew ? 'Add medicine' : 'Edit medicine'}"/><c:set var="nav" value="medicines"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/medicines">Medicines</a> / <c:out value="${isNew ? 'New' : medicine.medicineCode}"/></div>
        <h1><c:out value="${pageTitle}"/></h1>
    </div>
</div>
<div class="card">
    <div class="card-body">
        <ui:errors/>
        <form method="post" action="${ctx}/medicines/save" class="needs-validation row g-3" novalidate data-once>
            <ui:csrf/>
            <input type="hidden" name="id" value="${medicine.id}">
            <ui:input name="name" label="Medicine name" value="${medicine.name}" required="true" maxlength="150"/>
            <ui:input name="genericName" label="Generic name" value="${medicine.genericName}" maxlength="150"/>
            <ui:input name="category" label="Category" value="${medicine.category}" required="true" maxlength="80" col="col-md-4" list="categoryOptions" help="Pick an existing category or type a new one."/>
            <datalist id="categoryOptions"><c:forEach var="cat" items="${categories}"><option value="${fn:escapeXml(cat)}"></option></c:forEach></datalist>
            <ui:input name="dosageForm" label="Dosage form" value="${medicine.dosageForm}" required="true" maxlength="50" col="col-md-4" placeholder="Tablet, Capsule, Syrup..." list="formOptions"/>
            <datalist id="formOptions"><option value="Tablet"></option><option value="Capsule"></option><option value="Syrup"></option><option value="Injection"></option><option value="Ointment"></option><option value="Drops"></option><option value="Inhaler"></option></datalist>
            <ui:input name="strength" label="Strength" value="${medicine.strength}" maxlength="50" col="col-md-4" placeholder="e.g. 500 mg"/>
            <ui:input name="manufacturer" label="Manufacturer" value="${medicine.manufacturer}" maxlength="150"/>
            <ui:input name="unit" label="Unit of measurement" value="${medicine.unit}" required="true" maxlength="30" col="col-md-3" placeholder="tablet, bottle, tube..." help="The unit that is counted and sold."/>
            <ui:input name="reorderLevel" label="Reorder level" type="number" value="${reorderLevelValue}" required="true" min="0" step="1" col="col-md-3" help="Low-stock warning threshold."/>
            <ui:input name="description" label="Description" type="textarea" value="${medicine.description}" maxlength="500" col="col-12" rows="2"/>
            <div class="col-12 form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/medicines${isNew ? '' : '/view?id='}${isNew ? '' : medicine.id}">Cancel</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> <c:out value="${isNew ? 'Add medicine' : 'Save changes'}"/></button>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
