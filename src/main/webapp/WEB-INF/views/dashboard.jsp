<c:set var="pageTitle" value="Dashboard"/><c:set var="nav" value="dashboard"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div>
        <h1>Dashboard</h1>
        <p class="lead-text">Welcome back, <c:out value="${authUser.fullName}"/>. Here is the current state of the pharmacy.</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/patients/new"><ui:icon name="plus-lg"/> Register patient</a>
        <a class="btn btn-primary" href="${ctx}/dispensing"><ui:icon name="bag-check"/> Dispense</a>
    </div>
</div>

<section aria-label="Key figures">
    <div class="row g-3 mb-3">
        <div class="col-sm-6 col-xl-3"><a class="stat-card" href="${ctx}/patients"><span class="stat-icon"><ui:icon name="people"/></span><span><span class="stat-label">Registered patients</span><br><span class="stat-value">${summary.patients}</span></span></a></div>
        <div class="col-sm-6 col-xl-3"><a class="stat-card" href="${ctx}/doctors"><span class="stat-icon navy"><ui:icon name="person-badge"/></span><span><span class="stat-label">Doctors</span><br><span class="stat-value">${summary.doctors}</span></span></a></div>
        <div class="col-sm-6 col-xl-3"><a class="stat-card" href="${ctx}/prescriptions"><span class="stat-icon"><ui:icon name="file-medical"/></span><span><span class="stat-label">Prescriptions</span><br><span class="stat-value">${summary.prescriptions}</span></span></a></div>
        <div class="col-sm-6 col-xl-3"><a class="stat-card" href="${ctx}/medicines"><span class="stat-icon navy"><ui:icon name="capsule"/></span><span><span class="stat-label">Active medicines</span><br><span class="stat-value">${summary.activeMedicines}</span></span></a></div>
    </div>
    <div class="row g-3 mb-3">
        <div class="col-sm-6 col-xl-4"><a class="stat-card" href="${ctx}/medicines?stock=low"><span class="stat-icon ${summary.lowStock > 0 ? 'warn' : ''}"><ui:icon name="exclamation-triangle"/></span><span><span class="stat-label">Low-stock medicines</span><br><span class="stat-value">${summary.lowStock}</span></span></a></div>
        <div class="col-sm-6 col-xl-4"><a class="stat-card" href="${ctx}/inventory?status=near"><span class="stat-icon ${summary.nearExpiry > 0 ? 'warn' : ''}"><ui:icon name="clock-history"/></span><span><span class="stat-label">Batches expiring within ${summary.nearExpiryDays} days</span><br><span class="stat-value">${summary.nearExpiry}</span></span></a></div>
        <div class="col-sm-12 col-xl-4"><a class="stat-card" href="${ctx}/inventory?status=expired"><span class="stat-icon ${summary.expired > 0 ? 'bad' : ''}"><ui:icon name="calendar-x"/></span><span><span class="stat-label">Expired batches holding stock</span><br><span class="stat-value">${summary.expired}</span></span></a></div>
    </div>
    <div class="row g-3 mb-4">
        <div class="col-sm-6"><div class="stat-card"><span class="stat-icon"><ui:icon name="cash-coin"/></span><span><span class="stat-label">Today's sales</span><br><span class="stat-value"><small><c:out value="${cur}"/></small> ${f:money(summary.salesToday)}</span></span></div></div>
        <div class="col-sm-6"><div class="stat-card"><span class="stat-icon navy"><ui:icon name="graph-up"/></span><span><span class="stat-label">Sales this month</span><br><span class="stat-value"><small><c:out value="${cur}"/></small> ${f:money(summary.salesMonth)}</span></span></div></div>
    </div>
</section>

<div class="row g-3 mb-4">
    <div class="col-xl-7">
        <div class="card h-100">
            <div class="card-header">
                <h2>Sales trend</h2>
                <div class="segmented" role="group" aria-label="Sales period">
                    <button type="button" data-sales-range="7" aria-pressed="true">7 days</button>
                    <button type="button" data-sales-range="30" aria-pressed="false">30 days</button>
                    <button type="button" data-sales-range="90" aria-pressed="false">90 days</button>
                </div>
            </div>
            <div class="card-body">
                <div class="chart-box" id="salesChartBox">
                    <canvas role="img" aria-label="Line chart of invoiced sales per day" hidden></canvas>
                    <div class="chart-state" role="status"></div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-xl-5">
        <div class="card h-100">
            <div class="card-header"><h2>Usable stock by category</h2><span class="text-muted small">units on hand</span></div>
            <div class="card-body">
                <div class="chart-box" id="stockChartBox">
                    <canvas role="img" aria-label="Bar chart of stock units per medicine category" hidden></canvas>
                    <div class="chart-state" role="status"></div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row g-3">
    <div class="col-xl-4">
        <div class="card h-100">
            <div class="card-header"><h2>Recent prescriptions</h2><a class="small" href="${ctx}/prescriptions">View all</a></div>
            <c:choose>
                <c:when test="${empty summary.recentPrescriptions}"><ui:empty icon="file-medical" title="No prescriptions yet" text="Prescriptions will appear here once they are created."/></c:when>
                <c:otherwise>
                    <ul class="mini-list">
                        <c:forEach var="p" items="${summary.recentPrescriptions}">
                            <li>
                                <span><a class="primary" href="${ctx}/prescriptions/view?id=${p.id}"><c:out value="${p.prescriptionNo}"/></a>
                                    <span class="secondary"><c:out value="${p.patient.fullName}"/> &middot; ${f:date(p.prescriptionDate)}</span></span>
                                <ui:status value="${p.status}"/>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="col-xl-4">
        <div class="card h-100">
            <div class="card-header"><h2>Recent dispensing</h2><a class="small" href="${ctx}/dispensing/history">View all</a></div>
            <c:choose>
                <c:when test="${empty summary.recentDispensings}"><ui:empty icon="bag-check" title="Nothing dispensed yet" text="Dispensed prescriptions will be listed here."/></c:when>
                <c:otherwise>
                    <ul class="mini-list">
                        <c:forEach var="d" items="${summary.recentDispensings}">
                            <li>
                                <span><a class="primary" href="${ctx}/dispensing/view?id=${d.id}"><c:out value="${d.dispensingNo}"/></a>
                                    <span class="secondary"><c:out value="${d.patientName}"/> &middot; ${f:dateTime(d.dispensedAt)}</span></span>
                                <c:choose>
                                    <c:when test="${d.billed}"><span class="pill pill-ok">Billed</span></c:when>
                                    <c:otherwise><span class="pill pill-warn">Not billed</span></c:otherwise>
                                </c:choose>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="col-xl-4">
        <div class="card h-100">
            <div class="card-header"><h2>Recent invoices</h2><a class="small" href="${ctx}/invoices">View all</a></div>
            <c:choose>
                <c:when test="${empty summary.recentInvoices}"><ui:empty icon="receipt" title="No invoices yet" text="Invoices are created from dispensed prescriptions."/></c:when>
                <c:otherwise>
                    <ul class="mini-list">
                        <c:forEach var="i" items="${summary.recentInvoices}">
                            <li>
                                <span><a class="primary" href="${ctx}/invoices/view?id=${i.id}"><c:out value="${i.invoiceNo}"/></a>
                                    <span class="secondary"><c:out value="${cur}"/> ${f:money(i.grandTotal)} &middot; ${f:dateTime(i.createdAt)}</span></span>
                                <c:choose>
                                    <c:when test="${i.cancelled}"><ui:status value="CANCELLED"/></c:when>
                                    <c:otherwise><ui:status value="${i.paymentStatus}"/></c:otherwise>
                                </c:choose>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
<c:set var="pageScript" value="vendor/chart.umd.js,js/dashboard.js"/>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
