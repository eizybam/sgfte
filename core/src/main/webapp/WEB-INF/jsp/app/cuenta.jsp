<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${account.purpose}"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<a class="back" href="${pageContext.request.contextPath}/app/home">← Mis cuentas</a>

<h1 class="page-title">${account.purpose}</h1>
<p class="page-subtitle">${account.accountNumber}</p>

<div class="card card--feature mt-4" style="margin-bottom: var(--sp-3);">
    <div class="card__label">Saldo disponible</div>
    <div class="money money--xl" style="margin-top: var(--sp-1);">
        $ ${account.balance}<span class="money__currency">MXN</span>
    </div>
</div>

<%-- Tarjetas: puntos de acceso a la cuenta. Invalidar una NO mueve dinero (RN-06). --%>
<div class="card" style="margin-bottom: var(--sp-3);">
    <h2 class="card__title">Tarjetas de acceso</h2>
    <c:choose>
        <c:when test="${empty cards}">
            <p class="empty">Esta cuenta no tiene tarjetas emitidas.</p>
        </c:when>
        <c:otherwise>
            <div class="chips">
                <c:forEach var="k" items="${cards}">
                    <div class="chip ${k.status != 'ACTIVE' ? 'chip--inactive' : ''}">
                            ${k.maskedPan}
                        <small>
                            <c:choose>
                                <c:when test="${k.cardType == 'PHYSICAL'}">Física</c:when>
                                <c:otherwise>Digital</c:otherwise>
                            </c:choose>
                            ·
                            <c:choose>
                                <c:when test="${k.status == 'ACTIVE'}">Activa</c:when>
                                <c:when test="${k.status == 'BLOCKED'}">Bloqueada</c:when>
                                <c:otherwise>Inactiva</c:otherwise>
                            </c:choose>
                        </small>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%-- Ledger inmutable de esta cuenta (HU-08). Más reciente primero. --%>
<h2 class="card__title">Historial de movimientos</h2>
<div class="table-card">
    <table class="table">
        <thead>
        <tr><th>Fecha</th><th>Concepto</th><th>Descripción</th><th>Monto</th></tr>
        </thead>
        <tbody>
        <c:forEach var="m" items="${movements}">
            <tr>
                <td class="mono">${m.createdAt}</td>
                <td>
                    <c:choose>
                        <c:when test="${m.movementType == 'DEPOSIT'}"><span class="badge badge--ok">Depósito</span></c:when>
                        <c:when test="${m.movementType == 'TRANSFER_IN'}"><span class="badge badge--ok">Recibida</span></c:when>
                        <c:when test="${m.movementType == 'TRANSFER_OUT'}"><span class="badge badge--warn">Enviada</span></c:when>
                        <c:when test="${m.movementType == 'REINTEGRATION'}"><span class="badge badge--neutral">Reintegración</span></c:when>
                        <c:when test="${m.movementType == 'WITHDRAWAL'}"><span class="badge badge--error">Retiro</span></c:when>
                        <c:otherwise><span class="badge badge--neutral">${m.movementType}</span></c:otherwise>
                    </c:choose>
                </td>
                <td style="text-align:left;">${m.description}</td>
                <c:set var="incoming"
                       value="${m.movementType == 'DEPOSIT' or m.movementType == 'TRANSFER_IN'}"/>
                <td class="num ${incoming ? 'amount-in' : 'amount-out'}">
                    ${incoming ? '+' : '−'} $ ${m.amount}
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty movements}">
            <tr><td colspan="4" class="table__empty">Sin movimientos todavía.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
