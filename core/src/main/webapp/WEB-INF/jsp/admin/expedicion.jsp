<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Expedir Tarjetas"/>
<c:set var="pageSubtitle" value="Emite tarjetas físicas o digitales ligadas a una cuenta"/>
<c:set var="activeNav" value="cards"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="card" style="margin-bottom: var(--sp-3);">
    <h2 class="card__title">Nueva tarjeta</h2>
    <form method="post" action="${pageContext.request.contextPath}/admin/cards">
        <input type="hidden" name="action" value="issue">

        <div class="field">
            <label class="label" for="accountId">Cuenta</label>
            <select class="input" id="accountId" name="accountId" required>
                <option value="">— elige —</option>
                <c:forEach var="a" items="${accounts}">
                    <option value="${a.id}" ${a.id == selectedAccountId ? 'selected' : ''}>${a.label}</option>
                </c:forEach>
            </select>
        </div>

        <div class="field">
            <label class="label" for="cardType">Tipo</label>
            <select class="input" id="cardType" name="cardType" required>
                <option value="PHYSICAL">Física</option>
                <option value="DIGITAL">Digital</option>
            </select>
        </div>

        <div class="btn-pair">
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/home">Cancelar</a>
            <button type="submit" class="btn btn--primary">Expedir tarjeta</button>
        </div>
    </form>
</div>

<c:if test="${not empty cards}">
    <h2 class="card__title">Tarjetas de la cuenta</h2>
    <div class="table-card">
        <table class="table">
            <thead>
            <tr><th>ID</th><th>Tipo</th><th>Número</th><th>Estado</th><th>Acción</th></tr>
            </thead>
            <tbody>
            <c:forEach var="cd" items="${cards}">
                <tr>
                    <td class="mono">${cd.id}</td>
                    <td>
                        <c:choose>
                            <c:when test="${cd.cardType == 'PHYSICAL'}">Física</c:when>
                            <c:otherwise>Digital</c:otherwise>
                        </c:choose>
                    </td>
                    <td class="mono">${cd.maskedPan}</td>
                    <td>
                        <c:choose>
                            <c:when test="${cd.status == 'ACTIVE'}">
                                <span class="badge badge--ok">Activa</span>
                            </c:when>
                            <c:when test="${cd.status == 'BLOCKED'}">
                                <span class="badge badge--warn">Bloqueada</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge--error">Inactiva</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:if test="${cd.status == 'ACTIVE'}">
                            <%-- Invalidar NO mueve dinero: el saldo se queda en la cuenta (RN-06). --%>
                            <form method="post" action="${pageContext.request.contextPath}/admin/cards" style="margin:0;">
                                <input type="hidden" name="action" value="invalidate">
                                <input type="hidden" name="cardId" value="${cd.id}">
                                <input type="hidden" name="accountId" value="${selectedAccountId}">
                                <button type="submit" class="btn btn--link">Invalidar</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
