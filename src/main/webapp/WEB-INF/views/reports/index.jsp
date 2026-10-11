<c:set var="pageTitle" value="Reports"/><c:set var="nav" value="reports"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="page-head">
    <div><h1>Reports</h1><p class="lead-text">All figures are calculated from the database at the moment the report is opened.</p></div>
</div>
<h2 class="h5 mb-3">Sales and billing</h2>
<div class="row g-3 mb-4">
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/sales"><span class="stat-icon"><ui:icon name="graph-up"/></span><span><h2>Sales report</h2><p>Daily, weekly or monthly invoiced sales for any date range.</p></span></a></div>
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/billing"><span class="stat-icon"><ui:icon name="cash-coin"/></span><span><h2>Billing and payment status</h2><p>Invoices with amounts billed, received and outstanding.</p></span></a></div>
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/prescriptions"><span class="stat-icon navy"><ui:icon name="file-medical"/></span><span><h2>Prescription history</h2><p>Prescriptions by date and status.</p></span></a></div>
</div>
<h2 class="h5 mb-3">Stock</h2>
<div class="row g-3">
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/inventory"><span class="stat-icon navy"><ui:icon name="box-seam"/></span><span><h2>Medicine inventory</h2><p>Current stock of every medicine with its value at cost.</p></span></a></div>
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/low-stock"><span class="stat-icon warn"><ui:icon name="exclamation-triangle"/></span><span><h2>Low stock</h2><p>Medicines at or below their reorder level.</p></span></a></div>
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/expiry"><span class="stat-icon bad"><ui:icon name="calendar-x"/></span><span><h2>Expired and near-expiry batches</h2><p>Batches that need to be used first or removed.</p></span></a></div>
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/stock-movements"><span class="stat-icon navy"><ui:icon name="clock-history"/></span><span><h2>Stock movements</h2><p>Receipts, removals, adjustments and dispensing.</p></span></a></div>
    <div class="col-md-6 col-xl-4"><a class="report-tile" href="${ctx}/reports/supplier-receipts"><span class="stat-icon"><ui:icon name="truck"/></span><span><h2>Supplier stock receipts</h2><p>What was received from each supplier and its purchase value.</p></span></a></div>
</div>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
