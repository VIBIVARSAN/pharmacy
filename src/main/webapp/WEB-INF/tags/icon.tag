<%@ tag body-content="empty" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="css" required="false" %>
<svg class="icon ${css}" aria-hidden="true" focusable="false"><use href="${pageContext.request.contextPath}/assets/img/icons.svg#${name}"/></svg>
