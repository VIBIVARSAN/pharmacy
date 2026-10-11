<c:set var="pageTitle" value="Print ${prescription.prescriptionNo}"/><c:set var="nav" value="prescriptions"/>
<%@ include file="/WEB-INF/views/layout/header.jspf" %>
<div class="print-bar">
    <a class="btn btn-outline-secondary" href="${ctx}/prescriptions/view?id=${prescription.id}"><ui:icon name="arrow-left"/> Back to prescription</a>
    <button type="button" class="btn btn-primary" data-print><ui:icon name="printer"/> Print / Save as PDF</button>
</div>
<article class="doc">
    <header class="doc-head">
        <div>
            <h1><c:out value="${prescription.doctor.displayName}"/></h1>
            <div><c:out value="${prescription.doctor.specialization}"/></div>
            <c:if test="${not empty prescription.doctor.hospital}"><div><c:out value="${prescription.doctor.hospital}"/></div></c:if>
            <c:if test="${not empty prescription.doctor.registrationNo}"><div>Reg. No. <c:out value="${prescription.doctor.registrationNo}"/></div></c:if>
            <c:if test="${not empty prescription.doctor.phone}"><div>Phone: <c:out value="${prescription.doctor.phone}"/></div></c:if>
        </div>
        <div>
            <div class="doc-title">Prescription</div>
            <div class="text-end"><strong><c:out value="${prescription.prescriptionNo}"/></strong></div>
            <div class="text-end">Date: ${f:date(prescription.prescriptionDate)}</div>
            <c:if test="${prescription.status == 'CANCELLED' or prescription.status == 'DRAFT'}"><div class="text-end mt-2"><span class="watermark"><c:out value="${prescription.status.label}"/></span></div></c:if>
        </div>
    </header>
    <section class="doc-meta">
        <div>
            <h2>Patient</h2>
            <strong><c:out value="${prescription.patient.fullName}"/></strong> (<c:out value="${prescription.patient.patientCode}"/>)<br>
            <c:if test="${not empty prescription.patient.age}">${prescription.patient.age} years, </c:if><c:out value="${prescription.patient.gender.label}"/><br>
            <c:out value="${prescription.patient.phone}"/>
        </div>
        <div>
            <h2>Diagnosis</h2>
            <c:out value="${empty prescription.diagnosis ? '-' : prescription.diagnosis}"/>
        </div>
    </section>
    <table>
        <thead><tr><th scope="col">#</th><th scope="col">Medicine</th><th scope="col">Dosage</th><th scope="col">Frequency</th><th scope="col">Duration</th><th scope="col" class="num">Qty</th></tr></thead>
        <tbody>
        <c:forEach var="item" items="${prescription.items}" varStatus="row">
            <tr>
                <td>${row.count}</td>
                <td><strong><c:out value="${item.medicineName}"/></strong><c:if test="${not empty item.instructions}"><br><em><c:out value="${item.instructions}"/></em></c:if></td>
                <td><c:out value="${item.dosage}"/></td>
                <td><c:out value="${item.frequency}"/></td>
                <td><c:out value="${empty item.duration ? '-' : item.duration}"/></td>
                <td class="num">${item.quantity}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <c:if test="${not empty prescription.notes}"><p class="mt-3"><strong>Notes:</strong> <c:out value="${prescription.notes}"/></p></c:if>
    <footer class="doc-foot">
        <div>Recorded at <c:out value="${settings.pharmacy_name}"/><br>Valid only with the doctor's signature.</div>
        <div class="signature">Doctor's signature</div>
    </footer>
</article>
<%@ include file="/WEB-INF/views/layout/footer.jspf" %>
