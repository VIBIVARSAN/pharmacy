<%@ tag body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- Empty state shown instead of an empty table. The tag body can hold an action button. --%>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="text" required="false" %>
<div class="empty-state">
    <div class="empty-icon"><ui:icon name="${empty icon ? 'inbox' : icon}"/></div>
    <p class="empty-title"><c:out value="${title}"/></p>
    <c:if test="${not empty text}"><p class="empty-text"><c:out value="${text}"/></p></c:if>
    <jsp:doBody/>
</div>
