<%-- Entry point: the AuthFilter has already sent anonymous visitors to /login. --%>
<% response.sendRedirect(request.getContextPath() + "/dashboard"); %>
