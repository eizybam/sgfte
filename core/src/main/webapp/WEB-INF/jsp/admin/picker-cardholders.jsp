<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Fragmento: una página de la tabla del selector de empleados.

  NO es una página. No incluye admin-top.jspf ni admin-bottom.jspf: esto entra
  con innerHTML dentro de un modal que ya está abierto. Tampoco incluye
  icons.jspf — el sprite ya vive en la página anfitriona.

  La tabla es la de /admin/empleados (mismas clases .table--staff, mismas
  columnas) con dos diferencias, y las dos tienen motivo:

    · El nombre no es un <a> al detalle. Aquí no vas a navegar, vas a ELEGIR;
      un enlace te sacaría del formulario a medio llenar.
    · El paginador son <button>, no <a>. Un <a href> recargaría la página y
      cerraría el modal. Un botón sólo dispara el fetch de la página siguiente.

  El <tr> lleva en data-* lo que la pantalla necesita del empleado elegido, para
  que el script no tenga que volver a preguntar por él.
--%>
<table class="table table--staff picker__table">
    <thead>
    <tr>
        <th class="col-name">Empleado</th>
        <th class="col-id">ID</th>
        <th class="col-accs">Cuentas</th>
        <th class="col-cards">Tarjetas</th>
        <th class="col-funds">Fondo total</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="e" items="${rows}">
        <%-- tabindex + role: se puede llegar con Tab y elegir con Enter. --%>
        <tr class="picker__row" tabindex="0" role="button"
            data-id="${e.id}"
            data-name="${fn:escapeXml(e.fullName)}"
            data-code="${fn:escapeXml(e.employeeCode)}">
            <td>
                <span class="staff-name">${fn:escapeXml(e.fullName)}</span>
                <span class="staff-sub">${fn:escapeXml(e.email)}</span>
            </td>
            <td class="mono">${fn:escapeXml(e.employeeCode)}</td>
            <td class="num"><fmt:formatNumber value="${e.accountCount}" minIntegerDigits="2"/></td>
            <td class="num">${e.cardCount}</td>
            <td>
                $<fmt:formatNumber value="${e.totalFunds}" type="number"
                                   groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/> MXN
            </td>
        </tr>
    </c:forEach>
    <c:if test="${empty rows}">
        <tr><td colspan="5" class="table__empty">
            <%-- Dos criterios, dos mensajes: "no coincide" y "no coincide y
                 además tiene que tener cuenta" mandan a buscar cosas distintas. --%>
            ${onlyWithAccounts
                ? 'Ningún empleado activo CON CUENTAS coincide con la búsqueda.'
                : 'Ningún empleado activo coincide con la búsqueda.'}
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

                <%-- Misma ventana de 3 páginas que las tablas grandes. --%>
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