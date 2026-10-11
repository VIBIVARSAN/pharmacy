<c:set var="pageTitle" value="My profile"/><c:set var="nav" value="profile"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head"><div><h1>My profile</h1></div></div>
<div class="row g-3">
    <div class="col-lg-5">
        <div class="card h-100">
            <div class="card-header"><h2>Account</h2></div>
            <div class="card-body">
                <dl class="detail-grid">
                    <div><dt>Name</dt><dd><c:out value="${authUser.fullName}"/></dd></div>
                    <div><dt>Username</dt><dd><c:out value="${authUser.username}"/></dd></div>
                    <div><dt>Role</dt><dd><c:out value="${authUser.role.label}"/></dd></div>
                    <div><dt>Email</dt><dd><c:out value="${empty authUser.email ? 'Not recorded' : authUser.email}"/></dd></div>
                    <div><dt>Last sign-in</dt><dd><c:out value="${empty authUser.lastLoginAt ? '-' : f:dateTime(authUser.lastLoginAt)}"/></dd></div>
                </dl>
                <p class="text-muted small mt-3 mb-0">Ask an administrator to change your name, contact details or role.</p>
            </div>
        </div>
    </div>
    <div class="col-lg-7">
        <div class="card h-100">
            <div class="card-header"><h2>Change password</h2></div>
            <div class="card-body">
                <ui:errors/>
                <form method="post" action="${ctx}/profile" class="needs-validation row g-3" novalidate data-once>
                    <ui:csrf/>
                    <ui:input name="currentPassword" label="Current password" type="password" required="true" maxlength="72" col="col-12" autocomplete="current-password"/>
                    <ui:input name="newPassword" label="New password" type="password" required="true" maxlength="72" col="col-md-6" autocomplete="new-password" help="At least 8 characters with a letter and a digit."/>
                    <ui:input name="confirmPassword" label="Confirm new password" type="password" required="true" maxlength="72" col="col-md-6" autocomplete="new-password"/>
                    <div class="col-12 form-actions"><button class="btn btn-primary" type="submit"><ui:icon name="key"/> Change password</button></div>
                </form>
            </div>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
