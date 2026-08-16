<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Fragmento: las <option> de "CUENTA DESTINO" para un solo empleado.

  Son literalmente las mismas <option> que antes escribía expedicion.jsp en el
  c:forEach de la línea 51, con los mismos data-*, sólo que ahora sólo se
  escriben las del empleado elegido en vez de las de la empresa entera.

  Que el servidor devuelva HTML y no JSON es lo que hace que paint() en
  expedicion.jsp no cambie ni una línea: sigue leyendo opt.dataset.purpose
  igual que siempre, sin enterarse de que las <option> llegaron por fetch.

  Ya no hace falta data-holder-id: la lista ya viene filtrada por empleado, y
  filtrar en el navegador era justamente lo que se está quitando.
--%>
<option value="" disabled selected>Selecciona la cuenta</option>
<c:forEach var="t" items="${targets}">
    <option value="${t.accountId}"
            data-holder="${fn:escapeXml(t.cardholderName)}"
            data-account="${fn:escapeXml(t.accountNumber)}"
            data-purpose="${fn:escapeXml(t.purpose)}"
            data-balance="$<fmt:formatNumber value="${t.balance}" type="number"
                    groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/> MXN"
        ${t.accountId == selectedAccountId ? 'selected' : ''}>Cuenta ${fn:escapeXml(t.purpose)} · ${fn:escapeXml(t.accountNumber)}</option>
</c:forEach>