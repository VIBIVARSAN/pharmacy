<c:set var="isNew" value="${empty formUser.id}"/>
<c:set var="pageTitle" value="${isNew ? 'Add user' : 'Edit user'}"/><c:set var="nav" value="users"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div>
        <div class="crumbs"><a href="${ctx}/users">Users</a> / <c:out value="${isNew ? 'New' : formUser.username}"/></div>
        <h1><c:out value="${pageTitle}"/></h1>
    </div>
</div>
<div class="card mb-4">
    <div class="card-header"><h2>Account</h2></div>
    <div class="card-body">
        <ui:errors/>
        <form method="post" action="${ctx}/users/save" class="needs-validation row g-3" novalidate data-once autocomplete="off">
            <ui:csrf/>
            <input type="hidden" name="id" value="${formUser.id}">
            <c:choose>
                <c:when test="${isNew}"><ui:input name="username" label="Username" value="${formUser.username}" required="true" maxlength="50" col="col-md-4" autocomplete="off" help="3-50 letters, digits, dot, dash or underscore. Cannot be changed later."/></c:when>
                <c:otherwise><div class="col-md-4"><label class="form-label" for="usernameRo">Username</label><input class="form-control" id="usernameRo" type="text" value="${fn:escapeXml(formUser.username)}" disabled></div></c:otherwise>
            </c:choose>
            <ui:input name="fullName" label="Full name" value="${formUser.fullName}" required="true" maxlength="120" col="col-md-4"/>
            <div class="col-md-4">
                <label class="form-label" for="f_role">Role <span class="req" aria-hidden="true">*</span></label>
                <select class="form-select ${not empty errors.role ? 'is-invalid' : ''}" id="f_role" name="role" required>
                    <c:forEach var="r" items="${roles}"><option value="${r}" ${formUser.role == r ? 'selected' : ''}><c:out value="${r.label}"/></option></c:forEach>
                </select>
                <c:if test="${not empty errors.role}"><div class="invalid-feedback d-block"><c:out value="${errors.role}"/></div></c:if>
            </div>
            <ui:input name="email" label="Email" type="email" value="${formUser.email}" maxlength="150" col="col-md-4"/>
            <ui:input name="phone" label="Phone" type="tel" value="${formUser.phone}" maxlength="20" col="col-md-4"/>
            <c:choose>
                <c:when test="${isNew}">
                    <div class="col-md-4"></div>
                    <ui:input name="password" label="Password" type="password" required="true" maxlength="72" col="col-md-4" autocomplete="new-password" help="At least 8 characters with a letter and a digit."/>
                    <ui:input name="confirmPassword" label="Confirm password" type="password" required="true" maxlength="72" col="col-md-4" autocomplete="new-password"/>
                </c:when>
                <c:otherwise>
                    <div class="col-md-4">
                        <label class="form-label" for="f_active">Status</label>
                        <select class="form-select" id="f_active" name="active">
                            <option value="true" ${formUser.active ? 'selected' : ''}>Active - can sign in</option>
                            <option value="false" ${formUser.active ? '' : 'selected'}>Deactivated - cannot sign in</option>
                        </select>
                        <div class="form-text">Users are never deleted, so their past activity stays traceable.</div>
                    </div>
                </c:otherwise>
            </c:choose>
            <div class="col-12 form-actions">
                <a class="btn btn-outline-secondary" href="${ctx}/users">Cancel</a>
                <button class="btn btn-primary" type="submit"><ui:icon name="check-lg"/> <c:out value="${isNew ? 'Create user' : 'Save changes'}"/></button>
            </div>
        </form>
    </div>
</div>
<c:if test="${not isNew}">
<div class="card">
    <div class="card-header"><h2>Reset password</h2></div>
    <div class="card-body">
        <p class="text-muted">Set a new password for this user and tell them in person. The old password stops working immediately.</p>
        <form method="post" action="${ctx}/users/reset-password" class="needs-validation row g-3" novalidate data-once autocomplete="off"
              data-confirm="Reset the password for ${fn:escapeXml(formUser.username)}?" data-confirm-label="Reset password">
            <ui:csrf/>
            <input type="hidden" name="id" value="${formUser.id}">
            <input type="hidden" name="returnTo" value="/users/edit?id=${formUser.id}">
            <div class="col-md-4"><label class="form-label" for="rp1">New password <span class="req" aria-hidden="true">*</span></label><input class="form-control" id="rp1" name="password" type="password" minlength="8" maxlength="72" required autocomplete="new-password"><div class="invalid-feedback">At least 8 characters with a letter and a digit.</div></div>
            <div class="col-md-4"><label class="form-label" for="rp2">Confirm new password <span class="req" aria-hidden="true">*</span></label><input class="form-control" id="rp2" name="confirmPassword" type="password" minlength="8" maxlength="72" required autocomplete="new-password"><div class="invalid-feedback">Repeat the new password.</div></div>
            <div class="col-md-4 d-flex align-items-end"><button class="btn btn-outline-primary" type="submit"><ui:icon name="key"/> Reset password</button></div>
        </form>
    </div>
</div>
</c:if>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
