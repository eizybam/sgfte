<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Notificaciones · todo lo que se le ha avisado por correo al tarjetahabiente.

  Un solo filtro (categoría) en la URL, igual que el historial de movimientos:
  una vista filtrada se puede enlazar y recargar. La lista se parte en
  secciones por día — HOY, AYER, o la fecha — comparando la clave de grupo de
  cada fila con la anterior según se recorre, en vez de que el DAO devuelva
  grupos ya armados: así el mismo query paginado sirve para pintar la lista.
--%>
<c:set var="pageTitle" value="Notificaciones"/>
<c:set var="activeNav" value="notifications"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<c:set var="baseUrl" value="${ctx}/app/notificaciones"/>

<header class="notif-head">
    <h1 class="hist-title">Notificaciones</h1>
</header>

<nav class="notif-filters" aria-label="Categoría">
    <a class="notif-filter ${empty cat ? 'is-active' : ''}" href="${baseUrl}?page=1">Todas</a>
    <a class="notif-filter ${cat == 'SEGURIDAD' ? 'is-active' : ''}"
       href="${baseUrl}?page=1&cat=SEGURIDAD">Seguridad</a>
    <a class="notif-filter ${cat == 'ADMINISTRATIVA' ? 'is-active' : ''}"
       href="${baseUrl}?page=1&cat=ADMINISTRATIVA">Administrativas</a>
</nav>

<c:choose>
    <c:when test="${empty rows}">
        <p class="notif-empty">
            <c:choose>
                <c:when test="${empty cat}">Todavía no tienes notificaciones.</c:when>
                <c:otherwise>No hay notificaciones en esta categoría.</c:otherwise>
            </c:choose>
        </p>
    </c:when>
    <c:otherwise>
        <c:set var="lastGroup" value=""/>
        <c:forEach var="n" items="${rows}" varStatus="loop">
            <c:if test="${n.groupKey != lastGroup}">
                <c:if test="${not loop.first}"></ul></c:if>
                <div class="notif-day"><span>${fn:escapeXml(n.groupLabel)}</span></div>
                <ul class="notif-list">
                <c:set var="lastGroup" value="${n.groupKey}"/>
            </c:if>
            <li class="notif-card">
                <span class="notif-card__icon">
                    <svg width="20" height="20" aria-hidden="true"><use href="#${n.icon}"/></svg>
                </span>
                <span class="notif-card__text">
                    <span class="notif-card__title">${fn:escapeXml(n.title)}</span>
                    <span class="notif-card__tag notif-card__tag--${fn:toLowerCase(n.category)}">${fn:escapeXml(n.categoryLabel)}</span>
                </span>
                <span class="notif-card__when">${fn:escapeXml(n.when)}</span>
            </li>
        </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>

<c:if test="${pageCount > 1}">
    <c:set var="catParam" value="${empty cat ? '' : '&cat='.concat(cat)}"/>
    <nav class="hist-pager" aria-label="Paginación">
        <a class="hist-pager__item ${page == 1 ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page - 1}${catParam}"
           aria-label="Anterior"><svg width="16" height="16" aria-hidden="true"><use href="#i-prev"/></svg></a>

        <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
        <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>
        <c:forEach var="p" begin="${from}" end="${to}">
            <a class="hist-pager__item ${p == page ? 'is-current' : ''}"
               href="${baseUrl}?page=${p}${catParam}">${p}</a>
        </c:forEach>
        <c:if test="${to < pageCount}"><span class="hist-pager__item is-gap">…</span></c:if>

        <a class="hist-pager__item ${page == pageCount ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page + 1}${catParam}"
           aria-label="Siguiente"><svg width="16" height="16" aria-hidden="true"><use href="#i-next"/></svg></a>
    </nav>
</c:if>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
