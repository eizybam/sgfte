<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Vista Global"/>
<c:set var="pageSubtitle" value="Resumen financiero y control de dispersión"/>
<c:set var="activeNav" value="overview"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="grid-2">

    <%-- Concentradora: fuente única de fondos (RN-02). Tarjeta destacada. --%>
    <div class="card card--feature">
        <div class="card__label">Cuenta Concentradora</div>
        <div class="money money--xl mt-4">
            $ ${concentratorBalance}<span class="money__currency">MXN</span>
        </div>
        <div class="row" style="margin-top: var(--sp-4);">
            <a class="btn btn--primary" href="${pageContext.request.contextPath}/admin/dispersion">
                <svg width="18" height="18"><use href="#i-transfer"/></svg> Depositar a cuenta
            </a>
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/concentradora">
                Fondear
            </a>
        </div>
    </div>

    <div class="stack">
        <div class="card kpi">
            <div class="kpi__head">
                <span class="card__label">Tarjetahabientes activos</span>
                <span class="muted"><svg width="20" height="20"><use href="#i-users"/></svg></span>
            </div>
            <div class="money money--lg">${activeCardholders}</div>
        </div>

        <div class="card kpi">
            <div class="kpi__head">
                <span class="card__label">Total en cuentas</span>
                <span class="muted"><svg width="20" height="20"><use href="#i-money"/></svg></span>
            </div>
            <div class="money money--lg">
                $ ${totalInAccounts}<span class="money__currency">MXN</span>
            </div>
        </div>
    </div>
</div>

<%-- Distribución por propósito. Los porcentajes se calculan en el servlet. --%>
<div class="card mt-4">
    <h2 class="card__title">Distribución de gasto</h2>
    <p class="empty" style="margin-top:0;">Análisis del propósito de los fondos asignados en el periodo actual.</p>

    <c:choose>
        <c:when test="${empty purposes}">
            <p class="empty">Todavía no hay fondos asignados a ninguna cuenta.</p>
        </c:when>
        <c:otherwise>
            <table class="table" style="margin-top: var(--sp-3);">
                <c:forEach var="p" items="${purposes}">
                    <tr>
                        <td style="text-align:left; width:200px;">
                            <span class="badge badge--p${p.colorIndex}">${p.purpose}</span>
                        </td>
                        <td>
                            <%-- Barra proporcional: alternativa accesible a la gráfica de pastel --%>
                            <div style="height:8px; background:var(--sgfte-bg); border-radius:999px; overflow:hidden;">
                                <div style="height:100%; width:${p.percent}%; background:var(--sgfte-purpose-${p.colorIndex});"></div>
                            </div>
                        </td>
                        <td class="num" style="width:140px;">$ ${p.amount}</td>
                        <td class="num" style="width:80px;">${p.percent}%</td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<div class="kpi-grid mt-4">
    <div class="card kpi">
        <span class="card__label">Cuentas activas</span>
        <div class="money money--lg">${activeAccounts}</div>
    </div>
    <div class="card kpi">
        <span class="card__label">Tarjetas activas</span>
        <div class="money money--lg">${activeCards}</div>
    </div>
    <div class="card kpi">
        <span class="card__label">Accesos rápidos</span>
        <div class="row" style="margin-top: var(--sp-1);">
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/cards">Expedir tarjeta</a>
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/logs">Ver logs</a>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
