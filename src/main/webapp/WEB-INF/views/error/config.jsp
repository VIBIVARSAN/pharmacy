<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Configuration needed</title>
    <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body class="auth-body">
<main class="auth-card error-card">
    <h1 class="h4">The application is not configured yet</h1>
    <p class="text-muted"><c:out value="${applicationScope.startupError}"/></p>
    <ol class="text-start small text-muted">
        <li>Import <code>database.sql</code> into MySQL.</li>
        <li>Copy <code>src/main/resources/db.properties.example</code> to <code>db.properties</code> and enter your database user and password (or set the <code>DB_URL</code>, <code>DB_USER</code> and <code>DB_PASSWORD</code> environment variables).</li>
        <li>Rebuild and redeploy the application, then reload this page.</li>
    </ol>
    <p class="small text-muted mb-0">See README.md for step-by-step instructions.</p>
</main>
</body>
</html>
