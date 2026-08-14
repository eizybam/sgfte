<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Logs y Auditoría · marco Figma "Vista de Logs" (253:43).

  Misma mecánica que las otras tablas: filtros y página en la URL, paginador de
  enlaces. Sólo lectura: la bitácora es inmutable también en la base, por
  disparador, así que no hay nada que editar aquí.

  La columna ACCIÓN no sale de la base: se resuelve desde AuditEvent, para que
  cambiar un texto no reescriba el histórico.
--%>
<c:set var="pageTitle" value="Logs y Auditoría"/>
<c:set var="pageSubtitle" value="Trazabilidad completa de eventos y acciones del sistema"/>
<c:set var="activeNav" value="logs"/>
<c:set var="pageAction">
<c:url var="exportUrl" value="/admin/logs.csv">
    <c:param name="q" value="${q}"/>
    <c:param name="sev" value="${sev}"/>
    <c:param name="moduleFilter" value="${moduleFilter}"/>
</c:url>
    <a  class="btn btn--primary btn--logs"
          href="/admin/logs.csv"  title="Exportación de logs pendiente">
        <svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <rect x="5" y="3" width="18" height="22" rx="2"/>
            <path d="M9 9h10M9 14h10M9 19h6"/>
        </svg>
        Exportar logs
    </a>
</c:set>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<c:set var="baseUrl" value="${ctx}/admin/logs"/>
<c:set var="qParam"   value="${empty q ? '' : '&q='.concat(q)}"/>
<c:set var="sevParam" value="${empty sev ? '' : '&sev='.concat(sev)}"/>
<c:set var="modParam" value="${empty moduleFilter ? '' : '&mod='.concat(moduleFilter)}"/>

<div class="toolbar">
    <form class="search" method="get" action="${baseUrl}">
        <svg class="search__icon" width="18" height="18" aria-hidden="true"><use href="#i-search"/></svg>
        <input class="input" type="search" name="q" value="${fn:escapeXml(q)}"
               placeholder="Buscar por usuario, origen o evento" aria-label="Buscar en la bitácora">
        <c:if test="${not empty sev}"><input type="hidden" name="sev" value="${sev}"></c:if>
        <c:if test="${not empty moduleFilter}"><input type="hidden" name="mod" value="${moduleFilter}"></c:if>
    </form>

    <nav class="segmented" aria-label="Nivel">
        <a class="segmented__item ${empty sev ? 'is-active' : ''}"
           href="${baseUrl}?page=1${qParam}${modParam}">Todos</a>
        <a class="segmented__item ${sev == 'INFO' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&sev=INFO${qParam}${modParam}">Info</a>
        <a class="segmented__item ${sev == 'ALERTA' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&sev=ALERTA${qParam}${modParam}">Alerta</a>
        <a class="segmented__item ${sev == 'CRIT' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&sev=CRIT${qParam}${modParam}">Crit</a>
    </nav>

    <%-- Los módulos salen de los que ya existen en la bitácora. --%>
    <label class="pill">
        <span>Módulo ·</span>
        <select class="pill__select" onchange="location.href=this.value;" aria-label="Filtrar por módulo">
            <option value="${baseUrl}?page=1${qParam}${sevParam}" ${empty moduleFilter ? 'selected' : ''}>TODOS</option>
            <c:forEach var="m" items="${modules}">
                <option value="${baseUrl}?page=1&mod=${m}${qParam}${sevParam}"
                        ${moduleFilter == m ? 'selected' : ''}>${fn:escapeXml(m)}</option>
            </c:forEach>
        </select>
        <svg class="pill__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <span class="toolbar__count">
        <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/>
        ${total == 1 ? 'evento' : 'eventos'}
    </span>
</div>

<div class="table-card">
    <table class="table table--logs">
        <thead>
        <tr>
            <th class="col-when">Fecha/Hora</th>
            <th class="col-level">Nivel</th>
            <th class="col-action">Acción</th>
            <th class="col-user">Usuario</th>
            <th class="col-module">Módulo</th>
            <th class="col-origin">Origen</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="l" items="${rows}">
            <tr>
                <td>
                    <span class="log-when">
                        <span>${l.dateLabel}</span>
                        <span>${l.timeLabel}</span>
                    </span>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${l.severity == 'CRIT'}">
                            <span class="badge sev sev--crit">CRIT</span>
                        </c:when>
                        <c:when test="${l.severity == 'ALERTA'}">
                            <span class="badge sev sev--alerta">ALERTA</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge sev sev--info">INFO</span>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td title="${fn:escapeXml(l.detail)}">${fn:escapeXml(l.action)}</td>
                <td><span class="log-user" title="${fn:escapeXml(l.actor)}">${fn:escapeXml(l.actor)}</span></td>
                <td>${fn:escapeXml(l.module)}</td>
                <td><span class="log-origin" title="${fn:escapeXml(l.ipAddress)}">${fn:escapeXml(l.ipAddress)}</span></td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}">
            <tr>
                <td colspan="6" class="table__empty">
                    <c:choose>
                        <c:when test="${empty q and empty sev and empty moduleFilter}">
                            La bitácora está vacía. Se irá llenando conforme se opere el sistema.
                        </c:when>
                        <c:otherwise>No hay eventos que coincidan con el filtro.</c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:if>
        </tbody>
    </table>
</div>

<c:if test="${pageCount > 1}">
    <nav class="pager" aria-label="Paginación">
        <a class="pager__item ${page == 1 ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page - 1}${qParam}${sevParam}${modParam}" aria-label="Anterior">
            <svg aria-hidden="true"><use href="#i-prev"/></svg>
        </a>

        <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
        <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>

        <c:forEach var="p" begin="${from}" end="${to}">
            <a class="pager__item ${p == page ? 'is-current' : ''}"
               href="${baseUrl}?page=${p}${qParam}${sevParam}${modParam}">${p}</a>
        </c:forEach>

        <c:if test="${to < pageCount}"><span class="pager__item pager__gap">…</span></c:if>

        <a class="pager__item ${page == pageCount ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page + 1}${qParam}${sevParam}${modParam}" aria-label="Siguiente">
            <svg aria-hidden="true"><use href="#i-next"/></svg>
        </a>
    </nav>
</c:if>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
