<%@ page isErrorPage="true" %>
<c:set var="code" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="detail" value="${requestScope['jakarta.servlet.error.message']}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Error ${code}</title>
    <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body class="auth-body">
<main class="auth-card error-card">
    <p class="error-code">${code}</p>
    <c:choose>
        <c:when test="${code == 403}">
            <h1 class="h4">Access denied</h1>
            <p class="text-muted"><c:out value="${not empty detail and fn:contains(detail, 'session') ? detail : 'Your account does not have permission to open this page or perform this action.'}"/></p>
        </c:when>
        <c:when test="${code == 404}">
            <h1 class="h4">Page not found</h1>
            <p class="text-muted">The page or record you asked for does not exist. It may have been moved or the link may be incorrect.</p>
        </c:when>
        <c:when test="${code == 405}">
            <h1 class="h4">Action not allowed</h1>
            <p class="text-muted">This address cannot be used in that way. Please use the buttons and links in the application.</p>
        </c:when>
        <c:when test="${code == 400 or code == 409}">
            <h1 class="h4">The request could not be completed</h1>
            <p class="text-muted"><c:out value="${empty detail ? 'Please go back and check what you entered.' : detail}"/></p>
        </c:when>
        <c:otherwise>
            <h1 class="h4">Something went wrong</h1>
            <p class="text-muted">An unexpected error occurred and has been written to the server log. No changes were saved. Please try again; if it keeps happening, contact your administrator.</p>
        </c:otherwise>
    </c:choose>
    <a class="btn btn-primary" href="${ctx}/dashboard">Back to dashboard</a>
</main>
</body>
</html>
