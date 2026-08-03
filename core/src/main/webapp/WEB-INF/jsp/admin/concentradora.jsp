<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Cuenta Concentradora"/>
<c:set var="pageSubtitle" value="Fuente única de fondos de la empresa"/>
<c:set var="activeNav" value="overview"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="grid-2">
    <div class="card card--feature">
        <div class="card__label">Saldo disponible</div>
        <div class="money money--xl mt-4">
            $ ${concentrator.balance}<span class="money__currency">MXN</span>
        </div>
    </div>

    <div class="card">
        <h2 class="card__title">Fondear</h2>
        <form method="post" action="${pageContext.request.contextPath}/admin/concentradora">
            <div class="field">
                <label class="label" for="amount">Monto a fondear (MXN)</label>
                <input class="input" type="number" step="0.01" min="0.01"
                       id="amount" name="amount" placeholder="10000.00" required>
            </div>
            <div class="btn-pair">
                <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/home">Cancelar</a>
                <button type="submit" class="btn btn--primary">Fondear</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
