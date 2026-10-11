<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- Summary of server-side validation errors at the top of a form. --%>
<c:if test="${not empty errors}">
<div class="alert alert-danger d-flex gap-2" role="alert">
    <ui:icon name="exclamation-triangle" css="mt-1 flex-shrink-0"/>
    <div>
        <strong>Please correct the following:</strong>
        <ul class="mb-0 ps-3">
            <c:forEach var="e" items="${errors}"><li><c:out value="${e.value}"/></li></c:forEach>
        </ul>
    </div>
</div>
</c:if>
