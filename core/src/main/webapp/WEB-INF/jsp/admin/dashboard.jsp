<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Analíticas · marco Figma "Analitica - Admin (v2)" (2029:6).

  El selector de periodo reencuadra TODA la pantalla, incluidas las
  comparaciones, que se miden contra la ventana inmediatamente anterior del
  mismo tamaño. Por eso los rótulos llevan el periodo elegido en lugar del
  "DEL MES" fijo del marco: con el selector activo, ese texto mentiría en tres
  de las cuatro opciones.

  Las gráficas son CSS puro y la dona un conic-gradient, igual que el pastel de
  la Vista Global. Sin librería de gráficas.
--%>
<c:set var="pageTitle" value="Analíticas"/>
<c:set var="activeNav" value="analytics"/>
<c:set var="hidePageHead" value="true"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<c:set var="baseUrl" value="${ctx}/admin/dashboard"/>

<header class="an-head">
    <div class="an-head__titles">
        <h1 class="page-title">Analíticas</h1>
        <p class="page-subtitle">Análisis financiero y dispersión de fondos en tiempo real</p>
    </div>

    <div class="an-head__actions">
        <%-- El periodo de la pantalla viaja al reporte: si estás viendo 12 meses,
     el archivo dice 12 meses. --%>
        <c:url var="exportUrl" value="/admin/analytics.csv">
            <c:param name="period" value="${period.code}"/>
        </c:url>
        <a class="btn btn--primary btn--export" href="${exportUrl}"
           title="Descargar el reporte del periodo seleccionado en CSV">
            <svg viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M9 2v8M5.5 7.5 9 11l3.5-3.5M3 13.5v1.5h12v-1.5"/>
            </svg>
            Exportar reporte
        </a>


        <nav class="period" aria-label="Periodo">
            <c:forEach var="p" items="${periods}">
                <a class="period__item ${p == period ? 'is-active' : ''}"
                   href="${baseUrl}?period=${p.code}">${p.label}</a>
            </c:forEach>
        </nav>
    </div>
</header>

<%-- ---- Fila de KPIs ------------------------------------------------------ --%>
<div class="an-kpis">

    <article class="an-card an-kpi">
        <p class="an-kpi__label">SALDO CONCENTRADORA</p>
        <svg class="an-kpi__icon" viewBox="0 0 20 20" fill="none" stroke="currentColor"
             stroke-width="1.4" aria-hidden="true">
            <rect x="1" y="4" width="18" height="12" rx="2"/><path d="M13 4v12"/><circle cx="16" cy="10" r="1.2"/>
        </svg>
        <p class="an-kpi__value">
            <span>$<fmt:formatNumber value="${concentratorBalance}" type="number"
                    groupingUsed="true" maxFractionDigits="0"/></span>
            <span class="an-kpi__currency">MXN</span>
        </p>
        <%-- El delta sale del ledger de la concentradora (migración V3). --%>
        <c:choose>
            <c:when test="${empty concentratorDelta}">
                <p class="an-kpi__delta an-kpi__delta--flat">— vs periodo anterior</p>
            </c:when>
            <c:otherwise>
                <p class="an-kpi__delta ${concentratorDelta ge 0 ? 'an-kpi__delta--up' : 'an-kpi__delta--down'}">
                    ${concentratorDelta ge 0 ? '+' : '−'}<fmt:formatNumber
                        value="${concentratorDelta lt 0 ? -concentratorDelta : concentratorDelta}"
                        maxFractionDigits="1"/>% vs periodo anterior
                </p>
            </c:otherwise>
        </c:choose>
    </article>

    <article class="an-card an-kpi">
        <p class="an-kpi__label">DISPERSIÓN · ${period.caps}</p>
        <svg class="an-kpi__icon" viewBox="0 0 20 20" fill="none" stroke="currentColor"
             stroke-width="1.4" aria-hidden="true">
            <rect x="1" y="5" width="18" height="10" rx="2"/><circle cx="10" cy="10" r="2.5"/>
        </svg>
        <p class="an-kpi__value">
            <span>$<fmt:formatNumber value="${dispersed}" type="number"
                    groupingUsed="true" maxFractionDigits="0"/></span>
            <span class="an-kpi__currency">MXN</span>
        </p>
        <c:choose>
            <c:when test="${empty dispersedDelta}">
                <p class="an-kpi__delta an-kpi__delta--flat">— vs periodo anterior</p>
            </c:when>
            <c:otherwise>
                <p class="an-kpi__delta ${dispersedDelta ge 0 ? 'an-kpi__delta--up' : 'an-kpi__delta--down'}">
                    ${dispersedDelta ge 0 ? '+' : '−'}<fmt:formatNumber
                        value="${dispersedDelta lt 0 ? -dispersedDelta : dispersedDelta}"
                        maxFractionDigits="1"/>% vs periodo anterior
                </p>
            </c:otherwise>
        </c:choose>
    </article>

    <article class="an-card an-kpi">
        <p class="an-kpi__label">TARJETAHABIENTES</p>
        <svg class="an-kpi__icon" viewBox="0 0 20 20" fill="none" stroke="currentColor"
             stroke-width="1.4" aria-hidden="true">
            <circle cx="7.5" cy="7" r="2.6"/><path d="M2.5 16c.8-2.6 2.7-4 5-4s4.2 1.4 5 4"/>
            <circle cx="14.5" cy="7.5" r="2"/><path d="M14 12.4c1.7.3 2.8 1.5 3.4 3.6"/>
        </svg>
        <p class="an-kpi__value"><span>${activeCardholders}</span></p>
        <p class="an-kpi__delta ${newCardholders gt 0 ? 'an-kpi__delta--up' : 'an-kpi__delta--flat'}">
            +${newCardholders} ${newCardholders == 1 ? 'nuevo' : 'nuevos'} en el periodo
        </p>
    </article>

    <article class="an-card an-kpi">
        <p class="an-kpi__label">TARJETAS ACTIVAS</p>
        <svg class="an-kpi__icon" viewBox="0 0 20 20" fill="none" stroke="currentColor"
             stroke-width="1.4" aria-hidden="true">
            <rect x="1" y="4.5" width="18" height="11" rx="2"/><path d="M1 8.5h18"/>
        </svg>
        <p class="an-kpi__value"><span>${activeCards}</span></p>
        <p class="an-kpi__delta an-kpi__delta--flat">
            ${physicalCards} físicas · ${digitalCards} digitales
        </p>
    </article>
</div>

<%-- ---- Dispersión + dona ------------------------------------------------- --%>
<div class="an-grid">

    <section class="an-card">
        <h2 class="an-card__title">Dispersión ${period.bucketsAreMonths() ? 'mensual' : 'del periodo'}</h2>
        <p class="an-card__sub">${period.label} · MXN</p>

        <c:set var="maxBar" value="0"/>
        <c:forEach var="b" items="${dispersionBars}">
            <c:if test="${b.value gt maxBar}"><c:set var="maxBar" value="${b.value}"/></c:if>
        </c:forEach>

        <div class="chart">
            <div class="chart__grid">
                <c:forEach var="i" begin="0" end="3">
                    <c:set var="pos" value="${i * 33.33}"/>
                    <div class="chart__line" style="top: ${pos}%;">
                        <span class="chart__tick">$<fmt:formatNumber
                                value="${maxBar * (100 - pos) / 100000}"
                                maxFractionDigits="0"/>k</span>
                    </div>
                </c:forEach>
            </div>
            <div class="chart__bars">
                <c:forEach var="b" items="${dispersionBars}">
                    <div class="chart__col ${b.latest ? 'is-latest' : ''}">
                        <div class="chart__bar ${b.latest ? 'is-latest' : ''}" style="height: ${b.percent}%;"></div>
                        <span class="chart__label">${b.label}</span>
                    </div>
                </c:forEach>
            </div>
        </div>
    </section>

    <section class="an-card">
        <h2 class="an-card__title">Distribución por propósito</h2>
        <p class="an-card__sub">% de la dispersión del periodo</p>

        <c:choose>
            <c:when test="${empty purposes}">
                <p class="an-empty">Sin dispersiones en el periodo.</p>
            </c:when>
            <c:otherwise>
                <c:set var="acc" value="0"/>
                <c:set var="stops"><c:forEach var="p" items="${purposes}" varStatus="s"><c:if test="${not s.first}">,</c:if>var(--sgfte-purpose-${p.colorIndex}) ${acc}% ${s.last ? 100 : acc + p.percent}%<c:set var="acc" value="${acc + p.percent}"/></c:forEach></c:set>

                <div class="donut-row">
                    <div class="donut" role="img" aria-label="Reparto de la dispersión por propósito"
                         style="background: conic-gradient(${stops});">
                        <div class="donut__center">
                            <span class="donut__total">$<fmt:formatNumber value="${dispersed / 1000}"
                                    maxFractionDigits="0"/>k</span>
                            <span class="donut__caption">${period.label}</span>
                        </div>
                    </div>

                    <div class="donut-legend">
                        <c:forEach var="p" items="${purposes}">
                            <div class="donut-legend__row">
                                <span class="donut-legend__swatch"
                                      style="background: var(--sgfte-purpose-${p.colorIndex});"></span>
                                <span class="donut-legend__name">${fn:escapeXml(p.purpose)}</span>
                                <span class="donut-legend__pct">${p.percent}%</span>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%-- ---- Actividad + top cuentas -------------------------------------------- --%>
<div class="an-grid">

    <section class="an-card">
        <div class="an-card__head">
            <h2 class="an-card__title">Actividad de transacciones</h2>
            <span class="an-card__note">
                <fmt:formatNumber value="${weekTotal}" type="number" groupingUsed="true"/> en el periodo
            </span>
        </div>
        <p class="an-card__sub">Transacciones por día de la semana</p>

        <c:set var="maxDay" value="0"/>
        <c:forEach var="b" items="${weekdayBars}">
            <c:if test="${b.value gt maxDay}"><c:set var="maxDay" value="${b.value}"/></c:if>
        </c:forEach>

        <div class="chart chart--week">
            <div class="chart__grid">
                <c:forEach var="i" begin="0" end="3">
                    <c:set var="pos" value="${i * 33.33}"/>
                    <div class="chart__line" style="top: ${pos}%;">
                        <span class="chart__tick"><fmt:formatNumber
                                value="${maxDay * (100 - pos) / 100}" maxFractionDigits="0"/></span>
                    </div>
                </c:forEach>
            </div>
            <div class="chart__bars">
                <c:forEach var="b" items="${weekdayBars}">
                    <div class="chart__col ${b.latest ? 'is-latest' : ''}">
                        <div class="chart__bar ${b.latest ? 'is-latest' : ''}" style="height: ${b.percent}%;"></div>
                        <span class="chart__label">${b.label}</span>
                    </div>
                </c:forEach>
            </div>
        </div>
    </section>

    <section class="an-card">
        <h2 class="an-card__title">Top cuentas por gasto</h2>
        <p class="an-card__sub">Mayor dispersión del periodo</p>

        <c:choose>
            <c:when test="${empty topSpenders}">
                <p class="an-empty">Sin dispersiones en el periodo.</p>
            </c:when>
            <c:otherwise>
                <div class="top-list">
                    <c:forEach var="t" items="${topSpenders}">
                        <div>
                            <div class="top-list__head">
                                <span class="top-list__name">${fn:escapeXml(t.holderName)}</span>
                                <span class="top-list__amount">$<fmt:formatNumber
                                        value="${t.amount / 1000}" maxFractionDigits="0"/>k</span>
                            </div>
                            <div class="top-list__track">
                                <div class="top-list__fill"
                                     style="width: ${t.percent}%; background: var(--sgfte-purpose-${t.purposeColor});"></div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%-- ---- Transferencias P2P -------------------------------------------------- --%>
<section class="an-card" style="margin-top: 24px;">
    <div class="p2p">
        <div>
            <p class="an-kpi__label">TRANSFERENCIAS P2P</p>
            <p class="p2p__amount">
                <span>$<fmt:formatNumber value="${transfersTotal}" type="number"
                        groupingUsed="true" maxFractionDigits="0"/></span>
                <span class="an-kpi__currency">MXN</span>
            </p>
            <p class="p2p__note">
                <fmt:formatNumber value="${transfersCount}" type="number" groupingUsed="true"/>
                ${transfersCount == 1 ? 'transferencia' : 'transferencias'}
                · solo entre cuentas del mismo propósito
            </p>
        </div>

        <div>
            <p class="an-kpi__label">DISTRIBUCIÓN POR PROPÓSITO</p>
            <c:choose>
                <c:when test="${empty transferPurposes}">
                    <p class="an-empty" style="margin: var(--sp-3) 0 0; text-align: left;">
                        Sin transferencias en el periodo.
                    </p>
                </c:when>
                <c:otherwise>
                    <div class="p2p__bar">
                        <c:forEach var="p" items="${transferPurposes}">
                            <span class="p2p__seg"
                                  style="width: ${p.percent}%; background: var(--sgfte-purpose-${p.colorIndex});"></span>
                        </c:forEach>
                    </div>
                    <div class="p2p__legend">
                        <c:forEach var="p" items="${transferPurposes}">
                            <span class="p2p__item">
                                <span class="p2p__dot"
                                      style="background: var(--sgfte-purpose-${p.colorIndex});"></span>
                                ${fn:escapeXml(p.purpose)} ${p.percent}%
                            </span>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</section>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
