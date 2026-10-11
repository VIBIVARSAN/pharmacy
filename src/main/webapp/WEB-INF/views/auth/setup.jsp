<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>First-time setup &middot; Pharmacy Prescription Management System</title>
    <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body class="auth-body">
<main class="auth-card auth-card-wide">
    <div class="auth-brand"><span class="brand-mark"><ui:icon name="heart-pulse"/></span></div>
    <h1 class="h4 text-center mb-1">First-time setup</h1>
    <p class="text-center text-muted mb-4">Create the first administrator account. This page is only available until that account exists.</p>

    <ui:errors/>

    <form method="post" action="${ctx}/setup" class="needs-validation row g-3" novalidate data-once>
        <ui:csrf/>
        <ui:input name="token" label="Setup token" type="password" required="true" col="col-12" autocomplete="off"
                  help="The SETUP_TOKEN value from your configuration, or the token printed in the Tomcat log at start-up."/>
        <ui:input name="fullName" label="Full name" value="${fullName}" required="true" maxlength="120" col="col-md-6" autocomplete="name"/>
        <ui:input name="username" label="Username" value="${username}" required="true" maxlength="50" col="col-md-6" autocomplete="username"
                  help="3-50 letters, digits, dot, dash or underscore."/>
        <ui:input name="password" label="Password" type="password" required="true" maxlength="72" col="col-md-6" autocomplete="new-password"
                  help="At least 8 characters with a letter and a digit."/>
        <ui:input name="confirmPassword" label="Confirm password" type="password" required="true" maxlength="72" col="col-md-6" autocomplete="new-password"/>
        <div class="col-12"><button class="btn btn-primary w-100" type="submit">Create administrator</button></div>
    </form>
</main>
<script src="${ctx}/assets/vendor/bootstrap.bundle.min.js"></script>
<script src="${ctx}/assets/js/app.js"></script>
</body>
</html>
