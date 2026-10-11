<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- A labelled form control with server-side error display. --%>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="value" required="false" type="java.lang.Object" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="required" required="false" type="java.lang.Boolean" %>
<%@ attribute name="maxlength" required="false" %>
<%@ attribute name="col" required="false" %>
<%@ attribute name="help" required="false" %>
<%@ attribute name="min" required="false" %>
<%@ attribute name="max" required="false" %>
<%@ attribute name="step" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="autocomplete" required="false" %>
<%@ attribute name="rows" required="false" %>
<%@ attribute name="list" required="false" %>
<c:set var="error" value="${errors[name]}"/>
<c:set var="inputType" value="${empty type ? 'text' : type}"/>
<div class="${empty col ? 'col-md-6' : col}">
    <label class="form-label" for="f_${name}"><c:out value="${label}"/><c:if test="${required}"> <span class="req" aria-hidden="true">*</span></c:if></label>
    <c:choose>
        <c:when test="${inputType == 'textarea'}">
            <textarea class="form-control ${not empty error ? 'is-invalid' : ''}" id="f_${name}" name="${name}" rows="${empty rows ? 3 : rows}"
                <c:if test="${not empty maxlength}">maxlength="${maxlength}"</c:if>
                <c:if test="${required}">required</c:if>
                <c:if test="${not empty error or not empty help}">aria-describedby="h_${name}"</c:if>><c:out value="${value}"/></textarea>
        </c:when>
        <c:otherwise>
            <input class="form-control ${not empty error ? 'is-invalid' : ''}" id="f_${name}" name="${name}" type="${inputType}"
                value="${inputType == 'password' ? '' : fn:escapeXml(value)}"
                <c:if test="${not empty maxlength}">maxlength="${maxlength}"</c:if>
                <c:if test="${not empty min}">min="${min}"</c:if>
                <c:if test="${not empty max}">max="${max}"</c:if>
                <c:if test="${not empty step}">step="${step}"</c:if>
                <c:if test="${not empty list}">list="${list}"</c:if>
                <c:if test="${not empty placeholder}">placeholder="${fn:escapeXml(placeholder)}"</c:if>
                <c:if test="${not empty autocomplete}">autocomplete="${autocomplete}"</c:if>
                <c:if test="${required}">required</c:if>
                <c:if test="${not empty error or not empty help}">aria-describedby="h_${name}"</c:if>>
        </c:otherwise>
    </c:choose>
    <c:choose>
        <c:when test="${not empty error}"><div class="invalid-feedback d-block" id="h_${name}"><c:out value="${error}"/></div></c:when>
        <c:when test="${not empty help}"><div class="form-text" id="h_${name}"><c:out value="${help}"/></div></c:when>
    </c:choose>
</div>
