<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ attribute name="data" required="true" type="com.pharmacy.util.Page" %>
<%@ attribute name="noun" required="false" %>
<c:if test="${data.total > 0}">
<div class="pager d-flex flex-wrap align-items-center justify-content-between gap-2">
    <div class="text-muted small">Showing ${data.from}&ndash;${data.to} of ${data.total} <c:out value="${empty noun ? 'records' : noun}"/></div>
    <c:if test="${data.totalPages > 1}">
    <nav aria-label="Pagination">
        <ul class="pagination pagination-sm mb-0">
            <li class="page-item ${data.hasPrevious ? '' : 'disabled'}">
                <c:choose>
                    <c:when test="${data.hasPrevious}"><a class="page-link" href="?${qsNoPage}page=${data.page - 1}" aria-label="Previous page"><ui:icon name="chevron-left"/></a></c:when>
                    <c:otherwise><span class="page-link" aria-hidden="true"><ui:icon name="chevron-left"/></span></c:otherwise>
                </c:choose>
            </li>
            <c:set var="first" value="${data.page - 2 < 1 ? 1 : data.page - 2}"/>
            <c:set var="last" value="${first + 4 > data.totalPages ? data.totalPages : first + 4}"/>
            <c:set var="first" value="${last - 4 < 1 ? 1 : last - 4}"/>
            <c:forEach var="n" begin="${first}" end="${last}">
                <li class="page-item ${n == data.page ? 'active' : ''}">
                    <a class="page-link" href="?${qsNoPage}page=${n}" <c:if test="${n == data.page}">aria-current="page"</c:if>>${n}</a>
                </li>
            </c:forEach>
            <li class="page-item ${data.hasNext ? '' : 'disabled'}">
                <c:choose>
                    <c:when test="${data.hasNext}"><a class="page-link" href="?${qsNoPage}page=${data.page + 1}" aria-label="Next page"><ui:icon name="chevron-right"/></a></c:when>
                    <c:otherwise><span class="page-link" aria-hidden="true"><ui:icon name="chevron-right"/></span></c:otherwise>
                </c:choose>
            </li>
        </ul>
    </nav>
    </c:if>
</div>
</c:if>
