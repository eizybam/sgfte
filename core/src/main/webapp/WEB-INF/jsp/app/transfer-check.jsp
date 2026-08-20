<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Fragmento: el veredicto sobre el identificador que se está tecleando.

  NO es una página. No incluye app-top.jspf ni app-bottom.jspf: esto entra con
  innerHTML dentro del modal de transferencia, que ya está abierto.

  data-ok es lo que lee el guion para habilitar el botón de confirmar. Va como
  atributo y no como una cadena que haya que interpretar: el HTML dice si el
  destino sirve, y el navegador sólo obedece.
--%>
<c:choose>
    <c:when test="${not empty peer}">
        <span class="xfer__check xfer__check--ok" data-ok="1">
            ✓ ${fn:escapeXml(peer.label)}
        </span>
    </c:when>
    <c:otherwise>
        <%-- Un solo mensaje para todos los casos: ver el porqué exacto sería
             poder preguntarle a la pantalla por las cuentas de los demás. --%>
        <span class="xfer__check xfer__check--bad">
            No hay ninguna cuenta con ese identificador para este propósito.
            Pídele a tu compañero el que aparece en su pantalla de cuenta.
        </span>
    </c:otherwise>
</c:choose>
