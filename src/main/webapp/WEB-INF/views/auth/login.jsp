<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in &middot; Pharmacy Prescription Management System</title>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body class="auth-body">
<main class="auth-card">
    <div class="auth-brand"><span class="brand-mark"><ui:icon name="heart-pulse"/></span></div>
    <h1 class="h4 text-center mb-1">Pharmacy Prescription Management</h1>
    <p class="text-center text-muted mb-4">Sign in to continue</p>

    <c:if test="${not empty error}">
        <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not empty flash}">
        <div class="alert alert-${flash.type == 'danger' ? 'danger' : 'success'}" role="status"><c:out value="${flash.message}"/></div>
    </c:if>
    <c:if test="${param.loggedOut == '1' and empty error and empty flash}">
        <div class="alert alert-success" role="status">You have been signed out.</div>
    </c:if>

    <form method="post" action="${ctx}/login" class="needs-validation" novalidate data-once>
        <ui:csrf/>
        <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <input class="form-control" id="username" name="username" type="text" maxlength="50" required autofocus
                   autocomplete="username" value="${fn:escapeXml(username)}">
            <div class="invalid-feedback">Enter your username.</div>
        </div>
        <div class="mb-4">
            <label class="form-label" for="password">Password</label>
            <input class="form-control" id="password" name="password" type="password" maxlength="72" required autocomplete="current-password">
            <div class="invalid-feedback">Enter your password.</div>
        </div>
        <button class="btn btn-primary w-100" type="submit"><ui:icon name="lock"/> Sign in</button>
    </form>
</main>
<script src="${ctx}/assets/vendor/bootstrap.bundle.min.js"></script>
<script src="${ctx}/assets/js/app.js"></script>
</body>
</html>
