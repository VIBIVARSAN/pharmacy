<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- Sortable column heading. Keeps the current filters and toggles the direction. --%>
<%@ attribute name="key" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="numeric" required="false" type="java.lang.Boolean" %>
<c:set var="active" value="${pageRequest.sort == key}"/>
<c:set var="nextDir" value="${active and not pageRequest.descending ? 'desc' : 'asc'}"/>
<th scope="col" class="${numeric ? 'text-end' : ''}" <c:if test="${active}">aria-sort="${pageRequest.descending ? 'descending' : 'ascending'}"</c:if>>
    <a class="sort-link ${active ? 'active' : ''}" href="?${qsNoSort}sort=${key}&amp;dir=${nextDir}">
        <c:out value="${label}"/>
        <c:choose>
            <c:when test="${active and pageRequest.descending}"><ui:icon name="arrow-down"/></c:when>
            <c:when test="${active}"><ui:icon name="arrow-up"/></c:when>
            <c:otherwise><ui:icon name="arrow-down-up" css="sort-idle"/></c:otherwise>
        </c:choose>
    </a>
</th>
