<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Historial de movimientos"/>
<c:set var="pageSubtitle" value="Registro inmutable de cada operación con dinero"/>
<c:set var="activeNav" value="logs"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<form method="get" action="${pageContext.request.contextPath}/admin/historial">
    <div class="toolbar">
        <div class="search" style="max-width:420px;">
            <select class="input" id="accountId" name="accountId" onchange="this.form.submit()"
                    style="padding-left: var(--sp-2);">
                <option value="">— elige una cuenta —</option>
                <c:forEach var="a" items="${accounts}">
                    <option value="${a.id}" ${a.id == selectedAccountId ? 'selected' : ''}>${a.label}</option>
                </c:forEach>
            </select>
        </div>
        <noscript><button type="submit" class="btn btn--secondary">Ver</button></noscript>
        <c:if test="${not empty selectedAccountId}">
            <span class="toolbar__count">${empty movements ? 0 : movements.size()} movimientos</span>
        </c:if>
    </div>
</form>

<c:choose>
    <c:when test="${empty selectedAccountId}">
        <div class="card">
            <p class="empty">Elige una cuenta para ver sus movimientos.</p>
        </div>
    </c:when>
    <c:otherwise>
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
                    <tr><td colspan="4" class="table__empty">Esta cuenta no tiene movimientos.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
