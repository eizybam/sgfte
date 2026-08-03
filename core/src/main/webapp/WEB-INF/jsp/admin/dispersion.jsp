<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Dispersión de fondos"/>
<c:set var="pageSubtitle" value="Envía dinero de la Concentradora a la cuenta de un empleado"/>
<c:set var="activeNav" value="accounts"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="card" style="margin-bottom: var(--sp-3);">
    <span class="card__label">Disponible en la Concentradora</span>
    <div class="money money--lg">$ ${concentrator.balance}<span class="money__currency">MXN</span></div>
</div>

<div class="card">
    <form method="post" action="${pageContext.request.contextPath}/admin/dispersion">
        <div class="field">
            <label class="label" for="accountId">Cuenta destino</label>
            <select class="input" id="accountId" name="accountId" required>
                <option value="">— elige —</option>
                <c:forEach var="a" items="${accounts}">
                    <option value="${a.id}">${a.label} · saldo $ ${a.balance}</option>
                </c:forEach>
            </select>
        </div>

        <div class="field">
            <label class="label" for="amount">Monto (MXN)</label>
            <input class="input" type="number" step="0.01" min="0.01"
                   id="amount" name="amount" placeholder="500.00" required>
        </div>

        <div class="field">
            <label class="label" for="description">Descripción (opcional)</label>
            <input class="input" type="text" id="description" name="description"
                   maxlength="200" placeholder="Gasolina agosto">
        </div>

        <div class="btn-pair">
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/home">Cancelar</a>
            <button type="submit" class="btn btn--primary">Dispersar a la cuenta</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
