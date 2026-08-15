<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Fragmento: una página de cuentas activas para el selector de dispersión.

  Es la tabla de /admin/cuentas con dos columnas cambiadas, y las dos por el
  mismo motivo — aquí no se está administrando, se está eligiendo a dónde va el
  dinero:

    · Fuera "Estado": todas las filas son ACTIVE, el servlet lo fija.
    · Fuera "Tarjetas", dentro "Saldo": cuántos plásticos cuelgan de la cuenta
      no ayuda a decidir un depósito; cuánto tiene, sí. AccountRow ya lo trae
      (lo usa el detalle del tarjetahabiente).

  El badge de propósito conserva su color: purposeColor sale de la consulta como
  un rank sobre el catálogo, así que "Gasolina" es del mismo color aquí, en la
  tabla grande y en el dashboard.

  Los data-* del <tr> son lo que la pantalla necesita para pintar la cuenta
  elegida sin volver a preguntar.
--%>
<table class="table table--accounts picker__table picker__table--accounts">
    <thead>
    <tr>
        <th class="col-code">Cuenta</th>
        <th class="col-holder">Titular</th>
        <th class="col-purpose">Propósito</th>
        <th class="col-balance">Saldo</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="a" items="${rows}">
        <tr class="picker__row" tabindex="0" role="button"
            data-id="${a.id}"
            data-number="${fn:escapeXml(a.accountNumber)}"
            data-holder="${fn:escapeXml(a.holderName)}"
            data-purpose="${fn:escapeXml(a.purpose)}">
            <td class="mono">${fn:escapeXml(a.accountNumber)}</td>
            <td>${fn:escapeXml(a.holderName)}</td>
            <td><span class="badge badge--p${a.purposeColor}">${fn:escapeXml(a.purpose)}</span></td>
            <td class="mono">
                $<fmt:formatNumber value="${a.balance}" type="number"
                                   groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/>
            </td>
        </tr>
    </c:forEach>
    <c:if test="${empty rows}">
        <tr><td colspan="4" class="table__empty">
            Ninguna cuenta activa coincide con la búsqueda.
        </td></tr>
    </c:if>
    </tbody>
</table>

<div class="picker__foot">
    <span class="toolbar__count">
        <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/>
        ${total == 1 ? 'resultado' : 'resultados'}
    </span>

    <c:if test="${pageCount > 1}">
        <nav class="pager" aria-label="Paginación del selector">
            <button type="button" class="pager__item ${page == 1 ? 'is-disabled' : ''}"
                    data-page="${page - 1}" aria-label="Anterior">
                <svg aria-hidden="true"><use href="#i-prev"/></svg>
            </button>

            <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
            <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>

            <c:forEach var="p" begin="${from}" end="${to}">
                <button type="button" class="pager__item ${p == page ? 'is-current' : ''}"
                        data-page="${p}">${p}</button>
            </c:forEach>

            <c:if test="${to < pageCount}"><span class="pager__item pager__gap">…</span></c:if>

            <button type="button" class="pager__item ${page == pageCount ? 'is-disabled' : ''}"
                    data-page="${page + 1}" aria-label="Siguiente">
                <svg aria-hidden="true"><use href="#i-next"/></svg>
            </button>
        </nav>
    </c:if>
</div>
