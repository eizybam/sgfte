<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Empleados"/>
<c:set var="pageSubtitle" value="Registra un nuevo tarjetahabiente en el sistema"/>
<c:set var="activeNav" value="people"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<%-- Esta pantalla usa successId, no el atributo genérico `success`. --%>
<c:if test="${not empty successId}">
    <div class="alert alert--ok">
        Tarjetahabiente registrado (ID ${successId}):
        <strong>${cardholder.firstName} ${cardholder.lastName}</strong> · ${cardholder.email}
    </div>
</c:if>

<div class="card">
    <h2 class="card__title">Alta de tarjetahabiente</h2>

    <form method="post" action="${pageContext.request.contextPath}/cardholders">
        <div class="field">
            <label class="label" for="firstName">Nombre</label>
            <input class="input" id="firstName" name="firstName" value="${cardholder.firstName}" required>
        </div>

        <div class="field">
            <label class="label" for="lastName">Apellido</label>
            <input class="input" id="lastName" name="lastName" value="${cardholder.lastName}" required>
        </div>

        <div class="field">
            <label class="label" for="email">Correo corporativo</label>
            <input class="input" type="email" id="email" name="email" value="${cardholder.email}" required>
        </div>

        <div class="field">
            <label class="label" for="phone">Teléfono <span class="muted">(opcional)</span></label>
            <input class="input" id="phone" name="phone" value="${cardholder.phone}">
        </div>

        <div class="btn-pair">
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/home">Cancelar</a>
            <button type="submit" class="btn btn--primary">Registrar</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
