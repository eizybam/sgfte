<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
  Vista Global · marco Figma "Vista principal de Admin" (201:22).

  La pantalla tiene tres piezas: la concentradora (fuente única de fondos,
  RN-02), dos KPIs y el reparto del dinero por propósito.

  Los importes se formatean con minFractionDigits=0 y maxFractionDigits=2, que
  es justo lo que hace el prototipo: 4250000 se ve "$4,250,000" y 853567.31 se
  ve "$853,567.31". No se redondea nada.
--%>
<c:set var="pageTitle" value="Vista Global"/>
<c:set var="pageSubtitle" value="Resumen financiero y control de dispersión"/>
<c:set var="activeNav" value="overview"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="vista-grid">

    <section class="conc">
        <p class="conc__label">CUENTA CONCENTRADORA</p>

        <p class="conc__amount">
            <span class="conc__figure">$<fmt:formatNumber value="${concentratorBalance}"
                    type="number" groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/></span>
            <span class="conc__currency">MXN</span>
        </p>

        <div class="conc__cta">
            <a class="btn btn--primary btn--hero" href="${ctx}/admin/dispersion">
                <img src="${ctx}/assets/img/icons/transfer.png" alt="">
                Depositar a cuenta
            </a>
        </div>
    </section>

    <div class="kpi-col">
        <article class="kpi-card">
            <div class="kpi-card__head">
                <span class="kpi-card__label">Tarjetahabientes activos</span>
                <img class="kpi-card__icon" src="${ctx}/assets/img/icons/users.png" alt="">
            </div>
            <p class="kpi-card__value">${activeCardholders}</p>
        </article>

        <article class="kpi-card">
            <div class="kpi-card__head">
                <span class="kpi-card__label">Dispersión mensual total</span>
                <img class="kpi-card__icon" src="${ctx}/assets/img/icons/cash.png" alt="">
            </div>
            <p class="kpi-card__value">
                <span>$<fmt:formatNumber value="${dispersionThisMonth}"
                        type="number" groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/></span>
                <span class="kpi-card__currency">MXN</span>
            </p>
        </article>
    </div>
</div>

<section class="dist">
    <h2 class="dist__title">Distribución de gasto</h2>
    <p class="dist__lead">Análisis del propósito de fondos<br>asignados en el periodo actual.</p>

    <c:choose>
        <c:when test="${empty purposes}">
            <p class="dist__empty">Todavía no hay fondos asignados a ninguna cuenta.</p>
        </c:when>
        <c:otherwise>
            <%--
              Paradas del degradado del pastel. Se acumulan los porcentajes ya
              redondeados en el servlet; la última rebanada se cierra en 100%
              para que un redondeo de 99 o 101 no deje un hueco ni se solape.
            --%>
            <c:set var="acc" value="0"/>
            <c:set var="pieStops"><c:forEach var="p" items="${purposes}" varStatus="s"><c:if test="${not s.first}">,</c:if>var(--sgfte-purpose-${p.colorIndex}) ${acc}% ${s.last ? 100 : acc + p.percent}%<c:set var="acc" value="${acc + p.percent}"/></c:forEach></c:set>

            <ul class="dist__legend">
                <c:forEach var="p" items="${purposes}">
                    <li class="dist__item">
                        <span class="dist__key">
                            <span class="dist__swatch"
                                  style="background: var(--sgfte-purpose-${p.colorIndex});"></span>
                            <span class="dist__name">${p.purpose}</span>
                        </span>
                        <span class="dist__pct">${p.percent}%</span>
                    </li>
                </c:forEach>
            </ul>

            <div class="dist__pie" role="img"
                 aria-label="Reparto del saldo por propósito"
                 style="background: conic-gradient(${pieStops});"></div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
