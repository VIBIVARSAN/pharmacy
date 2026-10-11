<c:set var="pageTitle" value="Search"/><c:set var="nav" value="search"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<c:set var="cur" value="${settings.currency_symbol}"/>
<div class="page-head">
    <div><h1>Search</h1>
        <c:if test="${searched}"><p class="lead-text">Results for "<c:out value="${q}"/>"</p></c:if>
    </div>
</div>
<c:choose>
<c:when test="${not searched}">
    <div class="card"><ui:empty icon="search" title="Search the whole system" text="Type at least 2 characters in the search box at the top: a patient name or phone, a doctor, a medicine, a prescription number or an invoice number."/></div>
</c:when>
<c:when test="${patients.total + doctors.total + medicines.total + prescriptions.total + invoices.total == 0}">
    <div class="card"><ui:empty icon="search" title="Nothing found" text="No patient, doctor, medicine, prescription or invoice matches that text."/></div>
</c:when>
<c:otherwise>
<div class="row g-3">
    <c:if test="${patients.total > 0}">
    <div class="col-xl-6"><div class="card h-100">
        <div class="card-header"><h2>Patients</h2><a class="small" href="${ctx}/patients?q=${qEncoded}">All ${patients.total}</a></div>
        <ul class="mini-list"><c:forEach var="p" items="${patients.items}">
            <li><span><a class="primary" href="${ctx}/patients/view?id=${p.id}"><c:out value="${p.fullName}"/></a><span class="secondary"><c:out value="${p.patientCode}"/> &middot; <c:out value="${p.phone}"/></span></span></li>
        </c:forEach></ul>
    </div></div>
    </c:if>
    <c:if test="${doctors.total > 0}">
    <div class="col-xl-6"><div class="card h-100">
        <div class="card-header"><h2>Doctors</h2><a class="small" href="${ctx}/doctors?q=${qEncoded}">All ${doctors.total}</a></div>
        <ul class="mini-list"><c:forEach var="d" items="${doctors.items}">
            <li><span><a class="primary" href="${ctx}/doctors/view?id=${d.id}"><c:out value="${d.displayName}"/></a><span class="secondary"><c:out value="${d.doctorCode}"/> &middot; <c:out value="${d.specialization}"/></span></span></li>
        </c:forEach></ul>
    </div></div>
    </c:if>
    <c:if test="${medicines.total > 0}">
    <div class="col-xl-6"><div class="card h-100">
        <div class="card-header"><h2>Medicines</h2><a class="small" href="${ctx}/medicines?q=${qEncoded}">All ${medicines.total}</a></div>
        <ul class="mini-list"><c:forEach var="m" items="${medicines.items}">
            <li><span><a class="primary" href="${ctx}/medicines/view?id=${m.id}"><c:out value="${m.label}"/></a><span class="secondary"><c:out value="${m.medicineCode}"/> &middot; <c:out value="${m.category}"/></span></span><span class="text-muted">${m.availableStock} in stock</span></li>
        </c:forEach></ul>
    </div></div>
    </c:if>
    <c:if test="${prescriptions.total > 0}">
    <div class="col-xl-6"><div class="card h-100">
        <div class="card-header"><h2>Prescriptions</h2><a class="small" href="${ctx}/prescriptions?q=${qEncoded}">All ${prescriptions.total}</a></div>
        <ul class="mini-list"><c:forEach var="rx" items="${prescriptions.items}">
            <li><span><a class="primary" href="${ctx}/prescriptions/view?id=${rx.id}"><c:out value="${rx.prescriptionNo}"/></a><span class="secondary"><c:out value="${rx.patient.fullName}"/> &middot; ${f:date(rx.prescriptionDate)}</span></span><ui:status value="${rx.status}"/></li>
        </c:forEach></ul>
    </div></div>
    </c:if>
    <c:if test="${invoices.total > 0}">
    <div class="col-xl-6"><div class="card h-100">
        <div class="card-header"><h2>Invoices</h2><a class="small" href="${ctx}/invoices?q=${qEncoded}">All ${invoices.total}</a></div>
        <ul class="mini-list"><c:forEach var="inv" items="${invoices.items}">
            <li><span><a class="primary" href="${ctx}/invoices/view?id=${inv.id}"><c:out value="${inv.invoiceNo}"/></a><span class="secondary"><c:out value="${inv.patientName}"/> &middot; <c:out value="${cur}"/> ${f:money(inv.grandTotal)}</span></span>
                <c:choose><c:when test="${inv.cancelled}"><ui:status value="CANCELLED"/></c:when><c:otherwise><ui:status value="${inv.paymentStatus}"/></c:otherwise></c:choose></li>
        </c:forEach></ul>
    </div></div>
    </c:if>
</div>
</c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
