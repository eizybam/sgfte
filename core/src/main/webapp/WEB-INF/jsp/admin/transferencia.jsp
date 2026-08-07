<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Transferencia P2P"/>
<c:set var="pageSubtitle" value="Solo entre cuentas del mismo propósito"/>
<c:set var="activeNav" value="accounts"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<%--
  Vista de administrador. El tarjetahabiente tiene la suya en /app/transferencia,
  donde el origen se limita a sus propias cuentas.
--%>
<div class="card">
    <form method="post" action="${pageContext.request.contextPath}/admin/transferencia">
        <div class="field">
            <label class="label" for="sourceId">Cuenta origen</label>
            <select class="input" id="sourceId" name="sourceId" required>
                <option value="">— elige —</option>
                <c:forEach var="a" items="${accounts}">
                    <option value="${a.id}">${a.label} · $ ${a.balance}</option>
                </c:forEach>
            </select>
        </div>

        <div class="field">
            <label class="label" for="destId">Cuenta destino</label>
            <select class="input" id="destId" name="destId" required>
                <option value="">— elige —</option>
                <c:forEach var="a" items="${accounts}">
                    <option value="${a.id}">${a.label} · $ ${a.balance}</option>
                </c:forEach>
            </select>
        </div>

        <div class="field">
            <label class="label" for="amount">Monto (MXN)</label>
            <input class="input" type="number" step="0.01" min="0.01"
                   id="amount" name="amount" placeholder="200.00" required>
        </div>

        <div class="field">
            <label class="label" for="description">Concepto (opcional)</label>
            <input class="input" type="text" id="description" name="description" maxlength="200">
        </div>

        <div class="btn-pair">
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/home">Cancelar</a>
            <button type="submit" class="btn btn--primary">Transferir</button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
