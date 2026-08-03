<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Gestión de Cuentas · marco Figma "Gestion de Cuentas" (225:41).

  Los tres filtros y la página viven en la URL, no en JavaScript: así una vista
  filtrada se puede enlazar y recargar, y el paginador son enlaces normales.

  El buscador es un formulario GET que arrastra el estado del segmentado y de la
  píldora en campos ocultos, para que buscar no borre los otros dos filtros.
--%>
<c:set var="pageTitle" value="Gestión de Cuentas"/>
<c:set var="pageSubtitle" value="Administra las cuentas y su dispersion de fondos"/>
<c:set var="activeNav" value="accounts"/>
<c:set var="pageAction">
    <a class="btn btn--primary btn--hero btn--stacked"
       href="${pageContext.request.contextPath}/accounts">
        <img src="${pageContext.request.contextPath}/assets/img/icons/plus.png" alt="">
        Crear<br>cuenta
    </a>
</c:set>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<%-- Base para rehacer la URL conservando los filtros que no se están tocando. --%>
<c:set var="baseUrl" value="${ctx}/admin/cuentas"/>
<c:set var="qParam"       value="${empty q ? '' : '&q='.concat(q)}"/>
<c:set var="statusParam"  value="${empty status ? '' : '&status='.concat(status)}"/>
<c:set var="purposeParam" value="${empty purpose ? '' : '&purpose='.concat(purpose)}"/>

<div class="toolbar">
    <form class="search" method="get" action="${baseUrl}">
        <svg class="search__icon" width="18" height="18" aria-hidden="true"><use href="#i-search"/></svg>
        <input class="input" type="search" name="q" value="${fn:escapeXml(q)}"
               placeholder="Buscar por titular o ID de cuenta" aria-label="Buscar cuentas">
        <c:if test="${not empty status}"><input type="hidden" name="status" value="${status}"></c:if>
        <c:if test="${not empty purpose}"><input type="hidden" name="purpose" value="${purpose}"></c:if>
    </form>

    <nav class="segmented" aria-label="Estado">
        <a class="segmented__item ${empty status ? 'is-active' : ''}"
           href="${baseUrl}?page=1${qParam}${purposeParam}">Todas</a>
        <a class="segmented__item ${status == 'ACTIVE' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&status=ACTIVE${qParam}${purposeParam}">Activas</a>
        <a class="segmented__item ${status == 'INACTIVE' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&status=INACTIVE${qParam}${purposeParam}">Inactivas</a>
    </nav>

    <%-- La píldora del marco es un desplegable; aquí es un <select> que navega. --%>
    <label class="pill">
        <span>Propósito ·</span>
        <select class="pill__select" onchange="location.href=this.value;" aria-label="Filtrar por propósito">
            <option value="${baseUrl}?page=1${qParam}${statusParam}" ${empty purpose ? 'selected' : ''}>TODOS</option>
            <c:forEach var="cat" items="${categories}">
                <option value="${baseUrl}?page=1&purpose=${cat.id}${qParam}${statusParam}"
                        ${purpose == cat.id ? 'selected' : ''}>${fn:escapeXml(cat.name)}</option>
            </c:forEach>
        </select>
        <svg class="pill__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <span class="toolbar__count">
        <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/>
        ${total == 1 ? 'resultado' : 'resultados'}
    </span>
</div>

<div class="table-card">
    <table class="table table--accounts">
        <thead>
        <tr>
            <th class="col-code">Cuenta</th>
            <th class="col-holder">Titular</th>
            <th class="col-purpose">Proposito</th>
            <th class="col-cards">Tarjetas</th>
            <th class="col-status">Estado</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${rows}">
            <tr>
                <%-- En el marco el código de cuenta es el enlace al detalle. --%>
                <td class="mono">
                    <a class="cell-link" href="${ctx}/admin/cuenta?id=${a.id}">${fn:escapeXml(a.accountNumber)}</a>
                </td>
                <td>${fn:escapeXml(a.holderName)}</td>
                <td><span class="badge badge--p${a.purposeColor}">${fn:escapeXml(a.purpose)}</span></td>
                <td class="num">${a.activeCards}</td>
                <td>
                    <c:choose>
                        <c:when test="${a.active}"><span class="badge badge--ok">Activa</span></c:when>
                        <c:otherwise><span class="badge badge--error">Inactiva</span></c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}">
            <tr><td colspan="5" class="table__empty">No hay cuentas que coincidan con el filtro.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<%-- Paginador: ventana de 3 páginas alrededor de la actual, con salto si faltan. --%>
<c:if test="${pageCount > 1}">
    <nav class="pager" aria-label="Paginación">
        <a class="pager__item ${page == 1 ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page - 1}${qParam}${statusParam}${purposeParam}" aria-label="Anterior">
            <svg aria-hidden="true"><use href="#i-prev"/></svg>
        </a>

        <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
        <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>

        <c:forEach var="p" begin="${from}" end="${to}">
            <a class="pager__item ${p == page ? 'is-current' : ''}"
               href="${baseUrl}?page=${p}${qParam}${statusParam}${purposeParam}">${p}</a>
        </c:forEach>

        <c:if test="${to < pageCount}"><span class="pager__item pager__gap">…</span></c:if>

        <a class="pager__item ${page == pageCount ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page + 1}${qParam}${statusParam}${purposeParam}" aria-label="Siguiente">
            <svg aria-hidden="true"><use href="#i-next"/></svg>
        </a>
    </nav>
</c:if>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
