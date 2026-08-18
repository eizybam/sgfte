<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Movimientos · la vista global del ledger. Sin marco de Figma: el prototipo
  nunca dibujó esta pantalla, así que toma prestada entera la mecánica de
  "Vista de Logs" —filtros y página en la URL, paginador de enlaces, sólo
  lectura— porque es la misma clase de tabla.

  Lo que cambia es la fuente: v_movement, que une el ledger de las cuentas con
  el de la Concentradora. Por eso existe la columna ÁMBITO, y por eso una
  dispersión aparece DOS veces: salida de la Concentradora y entrada a la
  cuenta. Es partida doble, no duplicación.

  Consecuencia: aquí NO se suma el dinero de la tabla en ningún sitio. Sumar
  las dos caras contaría cada peso dos veces, y un total que a veces miente es
  peor que no tener total. Lo que se muestra es el CONTEO.
--%>
<c:set var="pageTitle" value="Movimientos"/>
<c:set var="pageSubtitle" value="Cada movimiento de dinero del sistema, de los dos lados del libro"/>
<c:set var="activeNav" value="movements"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<c:set var="baseUrl" value="${ctx}/admin/movimientos"/>
<%--
  Cada filtro se arrastra en los enlaces de los demás: cambiar el ámbito no
  debe tirar el texto que se estaba buscando.

  Con <c:if> y no con el .concat() de logs.jsp porque dos de estos valores son
  Long, no String, y no hace falta averiguar si EL los va a convertir sin
  quejarse a media renderización.
--%>
<c:set var="qParam"    value=""/>
<c:set var="ambParam"  value=""/>
<c:set var="tipoParam" value=""/>
<c:set var="catParam"  value=""/>
<c:set var="ctaParam"  value=""/>
<c:set var="perParam"  value=""/>
<c:if test="${not empty q}">     <c:set var="qParam"    value="&q=${q}"/></c:if>
<c:if test="${not empty ambito}"><c:set var="ambParam"  value="&ambito=${ambito}"/></c:if>
<c:if test="${not empty tipo}">  <c:set var="tipoParam" value="&tipo=${tipo}"/></c:if>
<c:if test="${not empty cat}">   <c:set var="catParam"  value="&cat=${cat}"/></c:if>
<c:if test="${not empty cuenta}"><c:set var="ctaParam"  value="&cuenta=${cuenta}"/></c:if>
<c:if test="${period != 'TODOS'}"><c:set var="perParam" value="&period=${period}"/></c:if>

<div class="toolbar">
    <form class="search" method="get" action="${baseUrl}">
        <svg class="search__icon" width="18" height="18" aria-hidden="true"><use href="#i-search"/></svg>
        <input class="input" type="search" name="q" value="${fn:escapeXml(q)}"
               placeholder="Buscar por concepto, cuenta, titular o código"
               aria-label="Buscar en los movimientos">
        <%-- El buscador es un GET: sin esto, buscar borraría los demás filtros. --%>
        <c:if test="${not empty ambito}"><input type="hidden" name="ambito" value="${ambito}"></c:if>
        <c:if test="${not empty tipo}"><input type="hidden" name="tipo" value="${tipo}"></c:if>
        <c:if test="${not empty cat}"><input type="hidden" name="cat" value="${cat}"></c:if>
        <c:if test="${not empty cuenta}"><input type="hidden" name="cuenta" value="${cuenta}"></c:if>
        <c:if test="${period != 'TODOS'}"><input type="hidden" name="period" value="${period}"></c:if>
    </form>

    <nav class="segmented" aria-label="Ámbito">
        <a class="segmented__item ${empty ambito ? 'is-active' : ''}"
           href="${baseUrl}?page=1${qParam}${tipoParam}${catParam}${ctaParam}${perParam}">Todos</a>
        <a class="segmented__item ${ambito == 'CUENTA' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&ambito=CUENTA${qParam}${tipoParam}${catParam}${ctaParam}${perParam}">Cuentas</a>
        <a class="segmented__item ${ambito == 'CONCENTRADORA' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&ambito=CONCENTRADORA${qParam}${tipoParam}${catParam}${ctaParam}${perParam}">Concentradora</a>
    </nav>

    <%-- Los tipos salen de los que ya existen en el ledger, como los módulos
         de la bitácora: ofrecer un filtro que sólo puede dar cero filas es
         prometer algo que no hay. --%>
    <label class="pill">
        <span>Tipo ·</span>
        <select class="pill__select" onchange="location.href=this.value;" aria-label="Filtrar por tipo">
            <option value="${baseUrl}?page=1${qParam}${ambParam}${catParam}${ctaParam}${perParam}"
                    ${empty tipo ? 'selected' : ''}>TODOS</option>
            <c:forEach var="t" items="${types}">
                <option value="${baseUrl}?page=1&tipo=${t}${qParam}${ambParam}${catParam}${ctaParam}${perParam}"
                        ${tipo == t ? 'selected' : ''}>${t}</option>
            </c:forEach>
        </select>
        <svg class="pill__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <label class="pill">
        <span>Propósito ·</span>
        <select class="pill__select" onchange="location.href=this.value;" aria-label="Filtrar por propósito">
            <option value="${baseUrl}?page=1${qParam}${ambParam}${tipoParam}${ctaParam}${perParam}"
                    ${empty cat ? 'selected' : ''}>TODOS</option>
            <c:forEach var="k" items="${categories}">
                <option value="${baseUrl}?page=1&cat=${k.id}${qParam}${ambParam}${tipoParam}${ctaParam}${perParam}"
                        ${cat == k.id ? 'selected' : ''}>${fn:escapeXml(k.name)}</option>
            </c:forEach>
        </select>
        <svg class="pill__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <label class="pill">
        <span>Fecha ·</span>
        <select class="pill__select" onchange="location.href=this.value;" aria-label="Filtrar por periodo">
            <option value="${baseUrl}?page=1${qParam}${ambParam}${tipoParam}${catParam}${ctaParam}"
                    ${period == 'TODOS' ? 'selected' : ''}>TODOS</option>
            <option value="${baseUrl}?page=1&period=HOY${qParam}${ambParam}${tipoParam}${catParam}${ctaParam}"
                    ${period == 'HOY' ? 'selected' : ''}>HOY</option>
            <option value="${baseUrl}?page=1&period=7D${qParam}${ambParam}${tipoParam}${catParam}${ctaParam}"
                    ${period == '7D' ? 'selected' : ''}>7 DÍAS</option>
            <option value="${baseUrl}?page=1&period=30D${qParam}${ambParam}${tipoParam}${catParam}${ctaParam}"
                    ${period == '30D' ? 'selected' : ''}>30 DÍAS</option>
        </select>
        <svg class="pill__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <span class="toolbar__count">
        <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/>
        ${total == 1 ? 'movimiento' : 'movimientos'}
    </span>
</div>

<%--
  El filtro por cuenta llega desde el detalle de cuenta y no tiene control
  propio en la barra: sin este aviso sería invisible, y una tabla recortada por
  un filtro que no se ve parece una tabla rota.
--%>
<c:if test="${not empty cuenta}">
    <p class="moves-scope">
        Filtrando por una sola cuenta ·
        <a href="${baseUrl}?page=1${qParam}${ambParam}${tipoParam}${catParam}${perParam}">quitar el filtro</a>
    </p>
</c:if>

<div class="table-card">
    <table class="table table--moves">
        <thead>
        <tr>
            <%-- "Fecha" y no "Fecha/Hora": el encabezado es lo que fijaba el
                 ancho de esta columna —va en mono de 20px y no se parte—, y la
                 hora se lee igual debajo del día sin necesidad de anunciarla. --%>
            <th class="col-when">Fecha</th>
            <th class="col-scope">Ámbito</th>
            <th class="col-kind">Tipo</th>
            <th class="col-concept">Concepto</th>
            <th class="col-account">Cuenta</th>
            <th class="col-holder">Titular</th>
            <th class="col-purpose">Propósito</th>
            <th class="col-amount">Monto</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="m" items="${rows}">
            <tr>
                <td>
                    <span class="log-when">
                        <span>${m.dayLabel}</span>
                        <span>${m.timeLabel}</span>
                    </span>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${m.concentrator}">
                            <span class="badge badge--neutral">CONC</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge badge--ok">CUENTA</span>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td>${m.kind}</td>
                <td title="${fn:escapeXml(m.concept)}">
                    <span class="log-origin">${fn:escapeXml(m.concept)}</span>
                </td>
                <%--
                  La contraparte de una P2P va en su PROPIO renglón, como la
                  hora debajo de la fecha. En una sola línea "GAS-37157 →
                  GAS-52353" no cabe en la columna y lo que se recortaba era
                  justo el destino, que es lo único que el flecha existe para
                  decir. Sin ella la fila cuenta que salió dinero pero no
                  hacia dónde.
                --%>
                <td title="${fn:escapeXml(m.accountLabel)}${empty m.relatedNumber ? '' : ' → '.concat(m.relatedNumber)}">
                    <span class="moves-acct">
                        <span>${fn:escapeXml(m.accountLabel)}</span>
                        <c:if test="${not empty m.relatedNumber}">
                            <span class="moves-acct__to">→ ${fn:escapeXml(m.relatedNumber)}</span>
                        </c:if>
                    </span>
                </td>
                <td><span class="log-user" title="${fn:escapeXml(m.whoLabel)}">${fn:escapeXml(m.whoLabel)}</span></td>
                <td class="moves-purpose">${empty m.categoryName ? "—" : fn:escapeXml(m.categoryName)}</td>
                <td class="num ${m.inflow ? 'amount-in' : 'amount-out'}">
                    ${m.inflow ? '+' : '−'}$<fmt:formatNumber value="${m.amount}" type="number"
                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}">
            <tr>
                <td colspan="8" class="table__empty">
                    <c:choose>
                        <c:when test="${empty q and empty ambito and empty tipo and empty cat
                                        and empty cuenta and period == 'TODOS'}">
                            Todavía no hay movimientos. La tabla se llena en cuanto se fondee o se disperse.
                        </c:when>
                        <c:otherwise>No hay movimientos que coincidan con el filtro.</c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:if>
        </tbody>
    </table>
</div>

<c:set var="allParams" value="${qParam}${ambParam}${tipoParam}${catParam}${ctaParam}${perParam}"/>

<c:if test="${pageCount > 1}">
    <nav class="pager" aria-label="Paginación">
        <a class="pager__item ${page == 1 ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page - 1}${allParams}" aria-label="Anterior">
            <svg aria-hidden="true"><use href="#i-prev"/></svg>
        </a>

        <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
        <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>

        <c:forEach var="p" begin="${from}" end="${to}">
            <a class="pager__item ${p == page ? 'is-current' : ''}"
               href="${baseUrl}?page=${p}${allParams}">${p}</a>
        </c:forEach>

        <c:if test="${to < pageCount}"><span class="pager__item pager__gap">…</span></c:if>

        <a class="pager__item ${page == pageCount ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page + 1}${allParams}" aria-label="Siguiente">
            <svg aria-hidden="true"><use href="#i-next"/></svg>
        </a>
    </nav>
</c:if>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
