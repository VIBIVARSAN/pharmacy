<c:set var="isNew" value="${empty patient.id}"/>
<c:set var="pageTitle" value="${isNew ? 'Register patient' : 'Edit patient'}"/><c:set var="nav" value="patients"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/patients">Patients</a> / <c:out value="${isNew ? 'New' : patient.patientCode}"/></div>
        <h1><c:out value="${pageTitle}"/></h1>
    </div>
</div>
<div class="card">
    <div class="card-body">
        <ui:errors/>
        <form method="post" action="${ctx}/patients/save" class="needs-validation row g-3" novalidate data-once>
            <ui:csrf/>
            <input type="hidden" name="id" value="${patient.id}">
            <c:if test="${isNew}"><div class="col-12"><div class="alert alert-info mb-0">The patient ID is generated automatically when you save.</div></div></c:if>
            <ui:input name="fullName" label="Full name" value="${patient.fullName}" required="true" maxlength="120" autocomplete="off"/>
            <div class="col-md-3">
                <label class="form-label" for="f_gender">Gender <span class="req" aria-hidden="true">*</span></label>
                <select class="form-select ${not empty errors.gender ? 'is-invalid' : ''}" id="f_gender" name="gender" required>
                    <option value="">Select...</option>
                    <option value="MALE" ${patient.gender == 'MALE' ? 'selected' : ''}>Male</option>
                    <option value="FEMALE" ${patient.gender == 'FEMALE' ? 'selected' : ''}>Female</option>
                    <option value="OTHER" ${patient.gender == 'OTHER' ? 'selected' : ''}>Other</option>
                </select>
                <c:if test="${not empty errors.gender}"><div class="invalid-feedback d-block"><c:out value="${errors.gender}"/></div></c:if>
            </div>
            <ui:input name="dateOfBirth" label="Date of birth" type="date" value="${patient.dateOfBirth}" col="col-md-3" help="Optional. Age is calculated from it."/>
            <ui:input name="phone" label="Phone" type="tel" value="${patient.phone}" required="true" maxlength="20" col="col-md-4" placeholder="e.g. +91 98765 43210"/>
            <ui:input name="email" label="Email" type="email" value="${patient.email}" maxlength="150" col="col-md-4"/>
            <ui:input name="address" label="Address" type="textarea" value="${patient.address}" maxlength="300" col="col-12" rows="2"/>
            <div class="col-12 form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/patients${isNew ? '' : '/view?id='}${isNew ? '' : patient.id}">Cancel</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> <c:out value="${isNew ? 'Register patient' : 'Save changes'}"/></button>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
