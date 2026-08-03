<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Gestor de empleados · marco Figma "Gestor de empleados" (287:104).

  Misma mecánica que Gestión de Cuentas: filtros y página en la URL, paginador
  de enlaces, tabla canónica.

  Dos columnas del marco no tienen dato detrás y se resuelven aquí, no en la
  base: el departamento bajo el nombre (se muestra el correo, que es el otro
  dato identificativo real) y el código tipo "AM84920" de la columna ID (se
  muestra la clave primaria).
--%>
<c:set var="pageTitle" value="Empleados"/>
<c:set var="pageSubtitle" value="Gestion de tarjetahabientes de la empresa"/>
<c:set var="activeNav" value="people"/>
<c:set var="pageAction">
    <a class="btn btn--primary btn--fixed" href="${pageContext.request.contextPath}/cardholders">
        <svg width="22" height="16" aria-hidden="true"><use href="#i-user-plus"/></svg>
        Registrar Empleado
    </a>
</c:set>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<c:set var="baseUrl" value="${ctx}/admin/empleados"/>
<c:set var="qParam"      value="${empty q ? '' : '&q='.concat(q)}"/>
<c:set var="statusParam" value="${empty status ? '' : '&status='.concat(status)}"/>

<div class="toolbar">
    <form class="search" method="get" action="${baseUrl}">
        <svg class="search__icon" width="18" height="18" aria-hidden="true"><use href="#i-search"/></svg>
        <input class="input" type="search" name="q" value="${fn:escapeXml(q)}"
               placeholder="Buscar por nombre o ID de empleado" aria-label="Buscar empleados">
        <c:if test="${not empty status}"><input type="hidden" name="status" value="${status}"></c:if>
    </form>

    <nav class="segmented" aria-label="Estado">
        <a class="segmented__item ${empty status ? 'is-active' : ''}"
           href="${baseUrl}?page=1${qParam}">Todos</a>
        <a class="segmented__item ${status == 'ACTIVE' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&status=ACTIVE${qParam}">Activos</a>
        <a class="segmented__item ${status == 'INACTIVE' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&status=INACTIVE${qParam}">Inactivos</a>
    </nav>

    <%--
      El filtro por departamento del marco no tiene sobre qué filtrar: no existe
      la columna. Se deja a la vista, inerte y explicado, en lugar de fingir que
      funciona o de quitarlo del diseño.
    --%>
    <span class="pill is-inert" title="Requiere un campo de departamento en el catálogo de empleados">
        <span>Departamento ·</span>
        <select class="pill__select" disabled aria-label="Filtrar por departamento (no disponible)">
            <option>TODOS</option>
        </select>
        <svg class="pill__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </span>

    <span class="toolbar__count">
        <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/>
        ${total == 1 ? 'resultado' : 'resultados'}
    </span>
</div>

<div class="table-card">
    <table class="table table--staff">
        <thead>
        <tr>
            <th class="col-name">Empleado</th>
            <th class="col-id">ID</th>
            <th class="col-accs">Cuentas</th>
            <th class="col-cards">Tarjetas</th>
            <th class="col-funds">Fondo total</th>
            <th class="col-state">Estado</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="e" items="${rows}">
            <tr>
                <td>
                    <span class="staff-name">${fn:escapeXml(e.fullName)}</span>
                    <span class="staff-sub">${fn:escapeXml(e.email)}</span>
                </td>
                <td class="mono">${e.id}</td>
                <%-- El marco rellena con cero a dos dígitos: 02, 01, 07 --%>
                <td class="num"><fmt:formatNumber value="${e.accountCount}" minIntegerDigits="2"/></td>
                <td class="num">${e.cardCount}</td>
                <td>
                    $<fmt:formatNumber value="${e.totalFunds}" type="number"
                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/> MXN
                </td>
                <td>
                    <c:choose>
                        <c:when test="${e.active}"><span class="badge badge--ok">Activa</span></c:when>
                        <c:otherwise><span class="badge badge--error">Inactiva</span></c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}">
            <tr><td colspan="6" class="table__empty">No hay empleados que coincidan con el filtro.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<c:if test="${pageCount > 1}">
    <nav class="pager" aria-label="Paginación">
        <a class="pager__item ${page == 1 ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page - 1}${qParam}${statusParam}" aria-label="Anterior">
            <svg aria-hidden="true"><use href="#i-prev"/></svg>
        </a>

        <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
        <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>

        <c:forEach var="p" begin="${from}" end="${to}">
            <a class="pager__item ${p == page ? 'is-current' : ''}"
               href="${baseUrl}?page=${p}${qParam}${statusParam}">${p}</a>
        </c:forEach>

        <c:if test="${to < pageCount}"><span class="pager__item pager__gap">…</span></c:if>

        <a class="pager__item ${page == pageCount ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page + 1}${qParam}${statusParam}" aria-label="Siguiente">
            <svg aria-hidden="true"><use href="#i-next"/></svg>
        </a>
    </nav>
</c:if>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
