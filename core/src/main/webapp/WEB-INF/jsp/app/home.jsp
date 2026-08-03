<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Mis cuentas"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<h1 class="page-title">Mis cuentas</h1>
<p class="page-subtitle">El dinero vive en la cuenta, no en la tarjeta.</p>

<div class="card card--feature mt-4" style="margin-bottom: var(--sp-4);">
    <div class="card__label">Saldo total disponible</div>
    <div class="money money--xl" style="margin-top: var(--sp-1);">
        $ ${total}<span class="money__currency">MXN</span>
    </div>
</div>

<c:choose>
    <c:when test="${empty accounts}">
        <div class="card">
            <p class="empty">
                Todavía no tienes cuentas asignadas. Cuando tu administrador te asigne una,
                aparecerá aquí con su propósito y su saldo.
            </p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="account-grid">
            <c:forEach var="a" items="${accounts}">
                <a class="account-card" href="${pageContext.request.contextPath}/app/cuenta?id=${a.id}">
                    <div class="account-card__purpose">${a.purpose}</div>
                    <div class="account-card__number">${a.accountNumber}</div>
                    <div class="money money--lg">$ ${a.balance}</div>
                    <div class="account-card__cards">
                        <c:choose>
                            <c:when test="${a.activeCards == 0}">Sin tarjetas activas</c:when>
                            <c:when test="${a.activeCards == 1}">1 tarjeta activa</c:when>
                            <c:otherwise>${a.activeCards} tarjetas activas</c:otherwise>
                        </c:choose>
                    </div>
                </a>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
