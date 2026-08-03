<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Registros e historial"/>
<c:set var="pageSubtitle" value="Bitácora inmutable de eventos del sistema"/>
<c:set var="activeNav" value="logs"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="toolbar">
    <div class="search">
        <span class="search__icon"><svg width="18" height="18"><use href="#i-search"/></svg></span>
        <input class="input" type="search" placeholder="Buscar en la bitácora" disabled>
    </div>
    <div class="segmented">
        <span class="segmented__item is-active">Todos</span>
        <span class="segmented__item">Info</span>
        <span class="segmented__item">Alerta</span>
    </div>
    <span class="toolbar__count">${empty logs ? 0 : logs.size()} resultados</span>
</div>

<div class="table-card">
    <table class="table">
        <thead>
        <tr>
            <th>ID</th>
            <th>Fecha</th>
            <th>Evento</th>
            <th>Detalle</th>
            <th>Actor</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="l" items="${logs}">
            <tr>
                <td class="mono">${l.id}</td>
                <td class="mono">${l.createdAt}</td>
                <td><span class="badge badge--neutral">${l.eventType}</span></td>
                <td style="text-align:left;">${l.detail}</td>
                <td class="mono">${l.actor}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty logs}">
            <tr>
                <td colspan="5" class="table__empty">
                    Sin eventos registrados todavía.
                    <br><span class="empty">Los módulos empiezan a escribir aquí cuando se cablea AuditLogService.record(...).</span>
                </td>
            </tr>
        </c:if>
        </tbody>
    </table>
</div>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
