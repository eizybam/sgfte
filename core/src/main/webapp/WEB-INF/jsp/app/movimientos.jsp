<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Historial de movimientos · marco Figma 109:4.

  A diferencia del panel y de la vista de cuenta, esta pantalla ocupa el ancho
  completo: no hay columnas laterales, sólo el encabezado, dos totales, la barra
  de filtros y la tabla.

  Los cuatro filtros viajan en la URL y no en JavaScript, igual que en las
  tablas del admin: una vista filtrada se puede enlazar y recargar, y el
  paginador son enlaces normales.
--%>
<c:set var="pageTitle" value="Historial de movimientos"/>
<c:set var="activeNav" value="movements"/>
<c:set var="mainClass" value="pmain--wide"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<%-- Base para rehacer la URL conservando los filtros que no se están tocando. --%>
<c:set var="baseUrl" value="${ctx}/app/movimientos"/>
<c:set var="qParam"       value="${empty q ? '' : '&q='.concat(q)}"/>
<c:set var="dirParam"     value="${dir == 'ALL' ? '' : '&dir='.concat(dir)}"/>
<c:set var="accountParam" value="${empty account ? '' : '&account='.concat(account)}"/>
<c:set var="periodParam"  value="${period == 'TODOS' ? '' : '&period='.concat(period)}"/>

<header class="hist-head">
    <div>
        <h1 class="hist-title">Historial de movimientos</h1>
        <p class="hist-lead">
            Consulta una vista detallada de tus transacciones<br>realizadas en todas tus cuentas
        </p>
    </div>

    <%--
      Los dos totales salen de la MISMA consulta filtrada que la tabla, así que
      siempre corresponden a lo que se está viendo.
    --%>
    <div class="hist-kpis">
        <section class="pbalance">
            <p class="pbalance__label">Total Gastado</p>
            <p class="pbalance__figure">
                <span>$<fmt:formatNumber value="${totalSpent}" type="number" groupingUsed="true"
                        minFractionDigits="2" maxFractionDigits="2"/></span>
                <span class="pbalance__currency">MXN</span>
            </p>
            <p class="pbalance__delta">${total} ${total == 1 ? 'movimiento' : 'movimientos'} en el filtro</p>
        </section>

        <section class="pbalance">
            <p class="pbalance__label">Total Recibido</p>
            <p class="pbalance__figure">
                <span>$<fmt:formatNumber value="${totalReceived}" type="number" groupingUsed="true"
                        minFractionDigits="2" maxFractionDigits="2"/></span>
                <span class="pbalance__currency">MXN</span>
            </p>
            <p class="pbalance__delta">Suma de recargas y transferencias recibidas</p>
        </section>
    </div>
</header>

<div class="hist-bar">
    <form class="hist-search" method="get" action="${baseUrl}">
        <svg width="18" height="18" aria-hidden="true"><use href="#i-search"/></svg>
        <input type="search" name="q" value="${fn:escapeXml(q)}"
               placeholder="Buscar por nombre, ID o cuenta..." aria-label="Buscar movimientos">
        <c:if test="${dir != 'ALL'}"><input type="hidden" name="dir" value="${dir}"></c:if>
        <c:if test="${not empty account}"><input type="hidden" name="account" value="${account}"></c:if>
        <c:if test="${period != 'TODOS'}"><input type="hidden" name="period" value="${period}"></c:if>
    </form>

    <nav class="hist-seg" aria-label="Tipo de movimiento">
        <a class="hist-seg__item ${dir == 'ALL' ? 'is-active' : ''}"
           href="${baseUrl}?page=1${qParam}${accountParam}${periodParam}">TODOS</a>
        <a class="hist-seg__item ${dir == 'IN' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&dir=IN${qParam}${accountParam}${periodParam}">INGRESOS</a>
        <a class="hist-seg__item ${dir == 'OUT' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&dir=OUT${qParam}${accountParam}${periodParam}">EGRESOS</a>
    </nav>

    <%--
      Las píldoras del marco son desplegables; aquí el <select> va encima,
      transparente, y navega al elegir. El valor actual se escribe aparte: el
      texto del <select> es invisible, así que sin esto la píldora decía
      "CUENTA ·" y se comía el "TODAS" que el marco sí enseña.
    --%>
    <c:set var="accountLabel" value="TODAS"/>
    <c:forEach var="a" items="${myAccounts}">
        <c:if test="${account == a.id}">
            <c:set var="accountLabel" value="${fn:toUpperCase(a.purpose)}"/>
        </c:if>
    </c:forEach>

    <label class="hist-pill">
        <span>CUENTA · ${fn:escapeXml(accountLabel)}</span>
        <select onchange="location.href=this.value;" aria-label="Filtrar por cuenta">
            <option value="${baseUrl}?page=1${qParam}${dirParam}${periodParam}"
                    ${empty account ? 'selected' : ''}>TODAS</option>
            <c:forEach var="a" items="${myAccounts}">
                <option value="${baseUrl}?page=1&account=${a.id}${qParam}${dirParam}${periodParam}"
                        ${account == a.id ? 'selected' : ''}>${fn:toUpperCase(fn:escapeXml(a.purpose))}</option>
            </c:forEach>
        </select>
        <svg width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <c:set var="periodLabel" value="${period == 'HOY' ? 'HOY' : (period == '7D' ? '7 DÍAS' : (period == '30D' ? '30 DÍAS' : 'TODAS'))}"/>

    <label class="hist-pill">
        <span>FECHA · ${periodLabel}</span>
        <select onchange="location.href=this.value;" aria-label="Filtrar por fecha">
            <option value="${baseUrl}?page=1${qParam}${dirParam}${accountParam}"
                    ${period == 'TODOS' ? 'selected' : ''}>TODAS</option>
            <option value="${baseUrl}?page=1&period=HOY${qParam}${dirParam}${accountParam}"
                    ${period == 'HOY' ? 'selected' : ''}>HOY</option>
            <option value="${baseUrl}?page=1&period=7D${qParam}${dirParam}${accountParam}"
                    ${period == '7D' ? 'selected' : ''}>7 DÍAS</option>
            <option value="${baseUrl}?page=1&period=30D${qParam}${dirParam}${accountParam}"
                    ${period == '30D' ? 'selected' : ''}>30 DÍAS</option>
        </select>
        <svg width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <span class="hist-count">${total} ${total == 1 ? 'resultado' : 'resultados'}</span>
</div>

<div class="hist-card">
    <table class="hist-table">
        <thead>
        <tr>
            <th class="c-date">FECHA/HORA</th>
            <th class="c-concept">CONCEPTO</th>
            <th class="c-origin">ORIGEN</th>
            <th class="c-method">METODO</th>
            <th class="c-target">DESTINATARIO</th>
            <th class="c-amount">MONTO</th>
            <th class="c-state">ESTADO</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="m" items="${rows}">
            <tr>
                <td class="hist-date">
                    <span>${m.dayLabel}</span>
                    <span>${m.timeLabel}</span>
                </td>
                <td class="hist-concept">
                    <span class="hist-concept__main">${fn:escapeXml(m.concept)}</span>
                    <span class="hist-concept__sub">${fn:escapeXml(m.kind)}</span>
                </td>
                <td>${fn:escapeXml(m.purpose)}</td>
                <%--
                  El marco escribe "Transacción" en TODAS las filas, así que la
                  columna no distingue nada. No hay columna de método en
                  account_movement, de modo que se respeta el literal del
                  prototipo en vez de inventar formas de pago.
                --%>
                <td>Transacción</td>
                <td class="mono-cell">${fn:escapeXml(m.counterparty)}</td>
                <td class="hist-amount ${m.inflow ? 'hist-amount--in' : ''}">
                    ${m.inflow ? '+' : '-'}$<fmt:formatNumber value="${m.amount}" type="number"
                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/>
                </td>
                <%--
                  Siempre COMPLETADO. El marco dibuja también "PENDIENTE", pero
                  la fila se escribe dentro de la transacción que ya movió el
                  saldo: si está en la tabla, ocurrió.
                --%>
                <td><span class="state-badge state-badge--done">COMPLETADO</span></td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}">
            <tr><td colspan="7" class="hist-empty">No hay movimientos que coincidan con el filtro.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<c:if test="${pageCount > 1}">
    <nav class="hist-pager" aria-label="Paginación">
        <a class="hist-pager__item ${page == 1 ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page - 1}${qParam}${dirParam}${accountParam}${periodParam}"
           aria-label="Anterior"><svg width="16" height="16" aria-hidden="true"><use href="#i-prev"/></svg></a>

        <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
        <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>
        <c:forEach var="p" begin="${from}" end="${to}">
            <a class="hist-pager__item ${p == page ? 'is-current' : ''}"
               href="${baseUrl}?page=${p}${qParam}${dirParam}${accountParam}${periodParam}">${p}</a>
        </c:forEach>
        <c:if test="${to < pageCount}"><span class="hist-pager__item is-gap">…</span></c:if>

        <a class="hist-pager__item ${page == pageCount ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page + 1}${qParam}${dirParam}${accountParam}${periodParam}"
           aria-label="Siguiente"><svg width="16" height="16" aria-hidden="true"><use href="#i-next"/></svg></a>
    </nav>
</c:if>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
