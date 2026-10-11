<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://pharmacy.com/functions" %>
<%-- Coloured status pill for prescription / invoice / payment states. --%>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<c:set var="v" value="${value.toString()}"/>
<c:choose>
    <c:when test="${v == 'COMPLETED' or v == 'PAID' or v == 'ACTIVE'}"><c:set var="tone" value="ok"/></c:when>
    <c:when test="${v == 'PARTIALLY_DISPENSED' or v == 'PARTIALLY_PAID' or v == 'NEAR_EXPIRY' or v == 'LOW_STOCK'}"><c:set var="tone" value="warn"/></c:when>
    <c:when test="${v == 'CANCELLED' or v == 'UNPAID' or v == 'EXPIRED' or v == 'OUT_OF_STOCK'}"><c:set var="tone" value="bad"/></c:when>
    <c:when test="${v == 'ISSUED'}"><c:set var="tone" value="info"/></c:when>
    <c:otherwise><c:set var="tone" value="muted"/></c:otherwise>
</c:choose>
<span class="pill pill-${tone}"><c:out value="${f:label(v)}"/></span>
