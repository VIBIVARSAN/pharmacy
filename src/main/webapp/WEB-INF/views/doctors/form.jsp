<c:set var="isNew" value="${empty doctor.id}"/>
<c:set var="pageTitle" value="${isNew ? 'Add doctor' : 'Edit doctor'}"/><c:set var="nav" value="doctors"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/doctors">Doctors</a> / <c:out value="${isNew ? 'New' : doctor.doctorCode}"/></div>
        <h1><c:out value="${pageTitle}"/></h1>
    </div>
</div>
<div class="card">
    <div class="card-body">
        <ui:errors/>
        <form method="post" action="${ctx}/doctors/save" class="needs-validation row g-3" novalidate data-once>
            <ui:csrf/>
            <input type="hidden" name="id" value="${doctor.id}">
            <ui:input name="fullName" label="Full name" value="${doctor.fullName}" required="true" maxlength="120" help="Without the title - 'Dr.' is added automatically."/>
            <ui:input name="specialization" label="Specialization" value="${doctor.specialization}" required="true" maxlength="100"/>
            <ui:input name="phone" label="Phone" type="tel" value="${doctor.phone}" required="true" maxlength="20" col="col-md-4"/>
            <ui:input name="email" label="Email" type="email" value="${doctor.email}" maxlength="150" col="col-md-4"/>
            <ui:input name="registrationNo" label="Medical registration number" value="${doctor.registrationNo}" maxlength="50" col="col-md-4"/>
            <ui:input name="hospital" label="Hospital / clinic" value="${doctor.hospital}" maxlength="150" col="col-12"/>
            <div class="col-12 form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/doctors${isNew ? '' : '/view?id='}${isNew ? '' : doctor.id}">Cancel</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> <c:out value="${isNew ? 'Add doctor' : 'Save changes'}"/></button>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
