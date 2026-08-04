<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Dashboard del tarjetahabiente · marco Figma "Vista principal de
  Tarjetahabiente" (64:4).

  Tres columnas: el resumen y los accesos a la izquierda, las cuentas en medio y
  la actividad reciente a la derecha.

  De las tres, sólo la rejilla de cuentas se desplaza. El marco lo dibuja
  explícitamente —el grupo se llama "Cuentas - Ejemplo scroll" y lleva encima y
  debajo dos rectángulos opacos del color del panel— porque el título y los dos
  botones se quedan quietos mientras las tarjetas pasan por detrás. Aquí eso son
  un encabezado y un pie pegajosos, y el desplazamiento vive en el panel y no en
  la página.
--%>
<c:set var="pageTitle" value="Dashboard"/>
<c:set var="activeNav" value="home"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<div class="pgrid">

    <%-- ---- Columna izquierda: balance y accesos rápidos -------------------- --%>
    <aside class="prail">
        <section class="pbalance">
            <p class="pbalance__label">BALANCE TOTAL ASIGNADO</p>
            <p class="pbalance__figure">
                <span>$<fmt:formatNumber value="${total}" type="number" groupingUsed="true"
                        minFractionDigits="2" maxFractionDigits="2"/></span>
                <span class="pbalance__currency">MXN</span>
            </p>
            <%--
              El marco escribe "+2.4% vs mes anterior". El saldo de apertura del
              mes no se guarda en ninguna parte, así que se deduce restando al
              total lo que se ha movido desde el día 1. Sin nada con qué
              comparar —mes de alta, apertura en cero— va un guion en vez de un
              porcentaje inventado.
            --%>
            <p class="pbalance__delta ${monthChange lt 0 ? 'pbalance__delta--down' : ''}">
                <c:choose>
                    <c:when test="${empty monthChange}">Sin comparación con el mes anterior</c:when>
                    <c:otherwise>
                        ${monthChange ge 0 ? '+' : ''}<fmt:formatNumber value="${monthChange}"
                            type="number" minFractionDigits="1" maxFractionDigits="1"/>% vs mes anterior
                    </c:otherwise>
                </c:choose>
            </p>
        </section>

        <p class="pquick__title">ACCESOS RAPIDOS</p>
        <%--
          Los tres accesos van inertes: el marco los dibuja, pero ninguno tiene
          pantalla diseñada —ni "Mis tarjetas", ni "Soporte", ni "Politicas"—,
          así que enlazarlos sería mandar al empleado a un 404. Se ven, como el
          botón de exportar de Logs, y se encienden cuando exista su marco.
        --%>
        <nav class="pquick">
            <span class="pquick__item is-pending" title="Pantalla pendiente">
                <svg width="30" height="32" aria-hidden="true"><use href="#i-cards"/></svg>
                <span>Mis tarjetas</span>
            </span>
            <span class="pquick__item is-pending" title="Pantalla pendiente">
                <svg width="32" height="32" aria-hidden="true"><use href="#i-headset"/></svg>
                <span>Soporte</span>
            </span>
            <span class="pquick__item is-pending" title="Pantalla pendiente">
                <svg width="32" height="32" aria-hidden="true"><use href="#i-policy"/></svg>
                <span>Politicas</span>
            </span>
        </nav>
    </aside>

    <%-- ---- Columna central: saludo y panel de cuentas ---------------------- --%>
    <section class="pcenter">
        <c:set var="firstName" value="${fn:split(sessionScope.user.fullName, ' ')[0]}"/>
        <h1 class="pwelcome">Bienvenido de nuevo, ${fn:escapeXml(firstName)}</h1>
        <p class="plead">Aquí esta el resumen de los fondos que se te han asignado.</p>

        <div class="paccounts">
            <div class="paccounts__head">
                <h2>Cuentas</h2>
            </div>

            <div class="paccounts__scroll">
                <c:choose>
                    <c:when test="${empty accounts}">
                        <p class="paccounts__empty">
                            Todavía no tienes cuentas asignadas. En cuanto administración te
                            asigne una, aparecerá aquí.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <div class="paccounts__grid">
                            <c:forEach var="a" items="${accounts}">
                                <a class="pcard" href="${ctx}/app/cuenta?id=${a.id}">
                                    <svg class="pcard__icon" width="36" height="36" aria-hidden="true">
                                        <use href="#${a.icon}"/>
                                    </svg>
                                    <span class="pcard__purpose">${fn:toUpperCase(fn:escapeXml(a.purpose))}</span>
                                    <span class="pcard__amount">$<fmt:formatNumber value="${a.balance}"
                                            type="number" groupingUsed="true"
                                            minFractionDigits="2" maxFractionDigits="2"/></span>
                                    <span class="pcard__currency">MXN</span>
                                </a>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="paccounts__foot">
                <a class="btn btn--primary btn--hero btn--stacked" href="${ctx}/app/transferencia">
                    <svg width="24" height="24" aria-hidden="true"><use href="#i-arrows"/></svg>
                    <span>Transferir<br>a compañero</span>
                </a>
                <%-- Deshabilitado, no enlazado a ninguna parte: la pantalla de
                     Movimientos todavía no existe y /app/cuenta necesita un id.
                     Un botón apagado dice eso mejor que un enlace roto. --%>
                <button type="button" class="btn btn--secondary btn--hero btn--stacked" disabled
                        title="Pantalla de Movimientos pendiente">
                    <svg width="24" height="24" aria-hidden="true"><use href="#i-logs"/></svg>
                    <span>Ver<br>Movimientos</span>
                </button>
            </div>
        </div>
    </section>

    <%-- ---- Columna derecha: actividad reciente ----------------------------- --%>
    <aside class="pactivity">
        <div class="pactivity__head">
            <p class="pactivity__title">ACTIVIDAD RECIENTE</p>
            <svg class="pactivity__filter" width="21" height="21" aria-hidden="true"><use href="#i-sliders"/></svg>
        </div>

        <c:choose>
            <c:when test="${empty activity}">
                <p class="paccounts__empty">Sin movimientos todavía.</p>
            </c:when>
            <c:otherwise>
                <ul class="pactivity__list">
                    <c:forEach var="m" items="${activity}">
                        <li class="pact">
                            <span class="pact__icon">
                                <svg width="18" height="18" aria-hidden="true"><use href="#${m.icon}"/></svg>
                            </span>
                            <span class="pact__text">
                                <span class="pact__label ${m.inflow ? 'pact__label--in' : ''}">${fn:escapeXml(m.label)}</span>
                                <span class="pact__when">${fn:escapeXml(m.when)}</span>
                            </span>
                            <span class="pact__amount ${m.inflow ? 'pact__amount--in' : ''}">
                                ${m.inflow ? '+' : '-'}$<fmt:formatNumber value="${m.amount}" type="number"
                                    groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/>
                            </span>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </aside>
</div>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
