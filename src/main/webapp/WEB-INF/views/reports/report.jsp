<c:set var="pageTitle" value="${reportTitle}"/><c:set var="nav" value="reports"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="page" value="${result.page}"/>
<div class="page-head">
    <div>
        <div class="crumbs d-print-none"><a href="${ctx}/reports">Reports</a> / <c:out value="${reportTitle}"/></div>
        <h1><c:out value="${reportTitle}"/></h1>
        <p class="lead-text"><c:out value="${reportDescription}"/></p>
        <p class="d-none d-print-block mb-0"><c:out value="${settings.pharmacy_name}"/> &middot; Generated ${f:dateTime(generatedAt)}<c:if test="${showDates}"> &middot; Period: <c:out value="${empty from ? 'beginning' : f:date(from)}"/> to <c:out value="${empty to ? 'today' : f:date(to)}"/></c:if></p>
    </div>
    <div class="page-actions"><button type="button" class="btn btn-outline-secondary" data-print><ui:icon name="printer"/> Print</button></div>
</div>

<div class="card mb-3 d-print-none">
    <div class="card-body">
        <form class="toolbar" method="get" action="${ctx}/reports${reportPath}">
            <c:if test="${showDates}">
                <div><label for="from">From</label><input class="form-control" id="from" type="date" name="from" value="${from}"></div>
                <div><label for="to">To</label><input class="form-control" id="to" type="date" name="to" value="${to}"></div>
            </c:if>
            <c:if test="${not empty filterLabel}">
                <div>
                    <label for="filter"><c:out value="${filterLabel}"/></label>
                    <select class="form-select" id="filter" name="filter">
                        <c:forEach var="o" items="${filterOptions}"><option value="${fn:escapeXml(o.key)}" ${o.key == filter ? 'selected' : ''}><c:out value="${o.value}"/></option></c:forEach>
                    </select>
                </div>
            </c:if>
            <div>
                <label for="size">Rows per page</label>
                <select class="form-select" id="size" name="size">
                    <c:forEach var="n" items="10,25,50,100"><option value="${n}" ${pageRequest.size == n ? 'selected' : ''}>${n}</option></c:forEach>
                </select>
            </div>
            <button class="btn btn-primary" type="submit"><ui:icon name="funnel"/> Apply</button>
            <a class="btn btn-outline-secondary" href="${ctx}/reports${reportPath}">Reset</a>
        </form>
    </div>
</div>

<div class="totals-strip mb-3" aria-label="Totals for all matching records">
    <c:forEach var="t" items="${result.totals}">
        <div class="total"><span><c:out value="${t.key}"/></span><strong><c:out value="${t.value}"/></strong></div>
    </c:forEach>
</div>

<div class="card">
    <c:choose>
        <c:when test="${empty page.items}"><ui:empty icon="bar-chart" title="No data for this report" text="${showDates ? 'Nothing was recorded in the selected period. Try a wider date range.' : 'There is nothing to report yet.'}"/></c:when>
        <c:otherwise>
            <div class="table-wrap"><table class="table">
                <caption class="visually-hidden"><c:out value="${reportTitle}"/></caption>
                <thead><tr><c:forEach var="col" items="${result.columns}"><th scope="col" class="${col.numeric ? 'text-end' : ''}"><c:out value="${col.label}"/></th></c:forEach></tr></thead>
                <tbody>
                <c:forEach var="row" items="${page.items}">
                    <tr><c:forEach var="cell" items="${row}" varStatus="c"><td class="${result.columns[c.index].numeric ? 'text-end' : ''}"><c:out value="${cell}"/></td></c:forEach></tr>
                </c:forEach>
                </tbody>
            </table></div>
            <ui:pager data="${page}" noun="rows"/>
        </c:otherwise>
    </c:choose>
</div>
<p class="text-muted small mt-2 d-print-none">Totals cover all matching records, not only the rows on this page. To print everything, raise "Rows per page" first.</p>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
