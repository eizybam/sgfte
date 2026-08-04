<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Transferencia entre compañeros · marco Figma "Transferencia" (111:2).

  El marco NO lleva cabecera ni menú: es una pantalla de tarea, centrada y sin
  nada alrededor que distraiga mientras se mueve dinero. Por eso no usa el
  cascarón del portal y trae su propio documento.

  "Cuenta destino" es un desplegable y no el campo libre "Número de cuenta o
  CLABE" que dibuja el marco. Aquí no hay CLABEs: el destino tiene que ser una
  cuenta concreta del sistema Y del mismo propósito que la de origen, que es la
  regla que el propio subtítulo enuncia. Escribirla a mano sólo permitiría
  equivocarse; el desplegable ya viene filtrado por findPeersForTransfer.

  Y por eso el origen recarga la página al cambiar: los destinos elegibles
  dependen de él.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Transferencia · SGFTE</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;500;600;700&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${ctx}/assets/css/sgfte.css?v=${applicationScope.assetsVersion}">
    <%@ include file="/WEB-INF/jsp/partials/canvas-fit.jspf" %>
</head>
<body class="portal-body">
<%@ include file="/WEB-INF/jsp/partials/icons.jspf" %>

<div class="xfer">
    <h1 class="xfer__title">Transferencia</h1>
    <p class="xfer__lead">
        Solo puedes transferir entre cuentas del mismo propósito (ej. Gasolina → Gasolina).
    </p>

    <form class="xfer__card" method="post" action="${ctx}/app/transferencia">

        <label class="xfer__label" for="sourceId">CUENTA ORIGEN</label>
        <div class="xfer__control">
            <svg class="xfer__icon" width="16" height="12" aria-hidden="true"><use href="#i-card-slot"/></svg>
            <%--
              Al cambiar de origen se recarga con ?sourceId=: los destinos
              posibles son los del MISMO propósito, así que dependen de esta
              elección y no pueden calcularse antes de hacerla.
            --%>
            <select class="xfer__input" id="sourceId" name="sourceId" required
                    onchange="location.href='${ctx}/app/transferencia?sourceId=' + this.value;">
                <option value="" disabled ${empty sourceId ? 'selected' : ''}>Selecciona una cuenta</option>
                <c:forEach var="a" items="${accounts}">
                    <option value="${a.id}" ${sourceId == a.id ? 'selected' : ''}>
                        ${fn:escapeXml(a.purpose)} · ${fn:escapeXml(a.accountNumber)}
                    </option>
                </c:forEach>
            </select>
            <svg class="xfer__chev" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
        </div>

        <label class="xfer__label" for="destId">CUENTA DESTINO</label>
        <div class="xfer__control">
            <select class="xfer__input" id="destId" name="destId" required
                    ${empty sourceId ? 'disabled' : ''}>
                <c:choose>
                    <c:when test="${empty sourceId}">
                        <option value="" selected>Elige primero la cuenta de origen</option>
                    </c:when>
                    <c:when test="${empty peers}">
                        <option value="" selected>Ningún compañero tiene una cuenta de este propósito</option>
                    </c:when>
                    <c:otherwise>
                        <option value="" disabled selected>Selecciona la cuenta destino</option>
                        <c:forEach var="p" items="${peers}">
                            <option value="${p.accountId}">${fn:escapeXml(p.label)}</option>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </select>
            <svg class="xfer__chev" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
        </div>

        <label class="xfer__label" for="amount">MONTO</label>
        <div class="xfer__control">
            <svg class="xfer__icon" width="16.74" height="17" aria-hidden="true"><use href="#i-cash-app"/></svg>
            <input class="xfer__input" type="number" step="0.01" min="0.01"
                   id="amount" name="amount" placeholder="0.00" required>
        </div>

        <label class="xfer__label" for="description">CONCEPTO</label>
        <div class="xfer__control xfer__control--area">
            <textarea class="xfer__input" id="description" name="description"
                      maxlength="200" placeholder="Ej. Pago comida, renta..."></textarea>
        </div>

        <div class="xfer__actions">
            <a class="btn btn--secondary btn--hero" href="${ctx}/app/home">Cancelar</a>
            <button type="submit" class="btn btn--primary btn--hero">
                <svg width="24" height="24" aria-hidden="true"><use href="#i-arrows"/></svg>
                Confirmar
            </button>
        </div>
    </form>
</div>

<%-- Un rechazo vuelve aquí, así que esta pantalla también pinta la tarjeta. --%>
<%@ include file="/WEB-INF/jsp/partials/result-modal.jspf" %>
</body>
</html>
