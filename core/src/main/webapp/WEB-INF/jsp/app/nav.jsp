<%--
  Shared header for the employee area. Included by every /app view so the
  screens are navigable without typing URLs, and so the "who am I / log out"
  controls live in exactly one file.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="portal-nav">
    <div class="portal-nav__brand">
        <span class="portal-nav__mark">SGFTE</span>
        <span class="portal-nav__who">${sessionScope.user.fullName}</span>
    </div>
    <nav class="portal-nav__links">
        <a href="${pageContext.request.contextPath}/app/home">Mis cuentas</a>
        <a href="${pageContext.request.contextPath}/app/transferencia">Transferir</a>
        <form method="post" action="${pageContext.request.contextPath}/logout">
            <button type="submit" class="portal-nav__logout">Cerrar sesión</button>
        </form>
    </nav>
</div>
