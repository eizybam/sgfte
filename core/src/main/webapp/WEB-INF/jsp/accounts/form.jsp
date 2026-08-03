<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Gestión de Cuentas"/>
<c:set var="pageSubtitle" value="Asigna una cuenta con propósito a un tarjetahabiente"/>
<c:set var="activeNav" value="accounts"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<%-- Esta pantalla usa successId/successNumber, no el atributo genérico `success`. --%>
<c:if test="${not empty successId}">
    <div class="alert alert--ok">
        Cuenta creada: <strong>${successNumber}</strong> · saldo inicial $0.00 MXN.
    </div>
</c:if>

<div class="card">
    <h2 class="card__title">Nueva cuenta</h2>
    <p class="empty" style="margin-top:0;">El saldo inicia en $0.00 MXN; se fondea desde la Concentradora.</p>

    <form method="post" action="${pageContext.request.contextPath}/accounts">
        <div class="field">
            <label class="label" for="cardholderId">Tarjetahabiente</label>
            <select class="input" id="cardholderId" name="cardholderId" required>
                <option value="">— elige —</option>
                <c:forEach var="ch" items="${cardholders}">
                    <option value="${ch.id}" ${ch.id == selectedCardholderId ? 'selected' : ''}>
                            ${ch.lastName}, ${ch.firstName}
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="field">
            <label class="label" for="categoryId">Propósito</label>
            <select class="input" id="categoryId" name="categoryId" required>
                <option value="">— elige —</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.id}" ${cat.id == selectedCategoryId ? 'selected' : ''}>
                            ${cat.name}
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="btn-pair">
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/home">Cancelar</a>
            <button type="submit" class="btn btn--primary">Crear cuenta</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
