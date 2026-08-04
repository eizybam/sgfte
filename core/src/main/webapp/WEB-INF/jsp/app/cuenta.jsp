<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Vista de cuenta del tarjetahabiente · marco Figma "Vista de cuenta de
  Tarjetahabiente" (263:58).

  El panel de tarjetas NO se desplaza, al revés que el de cuentas del dashboard.
  Una cuenta tiene como mucho dos tarjetas —una física y una digital—, así que
  no hay nada por lo que desplazarse: caben las dos siempre. Esa regla la
  garantiza el índice uq_card_active_type (V6), y el servlet además elige una de
  cada tipo, de modo que aquí nunca llegan tres.

  Con las dos, se solapan como en el marco: la de delante enseña todos sus
  datos y la de detrás asoma. Al pulsar cualquiera de las dos se intercambian.
  Con una sola, ocupa el ancho entero: no hay nada detrás que insinuar.
--%>
<c:set var="pageTitle" value="Cuenta ${account.accountNumber}"/>
<c:set var="activeNav" value="home"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<c:set var="cardCount" value="${(empty physicalCard ? 0 : 1) + (empty digitalCard ? 0 : 1)}"/>

<div class="pgrid">

    <aside class="prail">
        <section class="pbalance">
            <p class="pbalance__label">BALANCE ASIGNADO A LA CUENTA</p>
            <p class="pbalance__figure">
                <span>$<fmt:formatNumber value="${account.balance}" type="number" groupingUsed="true"
                        minFractionDigits="2" maxFractionDigits="2"/></span>
                <span class="pbalance__currency">MXN</span>
            </p>
            <p class="pbalance__delta">${fn:escapeXml(account.purpose)}</p>
        </section>

        <p class="pquick__title">ACCESOS RAPIDOS</p>
        <%-- Inertes mientras no exista su pantalla, igual que en el dashboard. --%>
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

    <section class="pcenter">
        <div class="pacct-head">
            <div>
                <h1 class="pwelcome">Cuenta ${fn:escapeXml(account.accountNumber)}</h1>
                <p class="plead">Administra las tarjetas asignadas a tu cuenta de ${fn:escapeXml(account.purpose)}.</p>
            </div>
            <a class="btn btn--primary btn--hero btn--stacked" href="${ctx}/app/transferencia?sourceId=${account.id}">
                <svg width="24" height="24" aria-hidden="true"><use href="#i-arrows"/></svg>
                <span>Transferir<br>a compañero</span>
            </a>
        </div>

        <div class="paccounts pcards-panel">
            <div class="paccounts__head pcards-head">
                <h2>Tarjetas</h2>
                <c:if test="${cardCount gt 0}">
                    <p class="pcards-hint">
                        ${cardCount eq 2
                            ? 'Da click en cualquier tarjeta para ver sus detalles completos.'
                            : 'Esta cuenta tiene una sola tarjeta.'}
                    </p>
                </c:if>
            </div>

            <div class="pcards ${cardCount eq 1 ? 'pcards--one' : ''}">
                <c:choose>
                    <c:when test="${cardCount eq 0}">
                        <p class="paccounts__empty">
                            Esta cuenta todavía no tiene tarjetas. Administración las expide.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <%--
                          Los dos radios van ANTES que las tarjetas para poder
                          seleccionarlas con ~. Son radios de verdad y no un
                          onclick: el intercambio funciona sin JavaScript y se
                          recorre con el teclado.
                        --%>
                        <c:if test="${cardCount eq 2}">
                            <input class="pcards__pick" type="radio" name="frontCard" id="front-a" checked>
                            <input class="pcards__pick" type="radio" name="frontCard" id="front-b">
                        </c:if>

                        <%-- Física primero: es la que el marco pone delante. --%>
                        <c:if test="${not empty physicalCard}">
                            <label class="tarjeta tarjeta--a" for="front-a">
                                <span class="tarjeta__top">
                                    <span class="tarjeta__key">TIPO DE LA TARJETA</span>
                                    <span class="tarjeta__pill">FÍSICA</span>
                                </span>
                                <span class="tarjeta__data">
                                    <span class="tarjeta__pan">
                                        <span class="tarjeta__key">NÚMERO DE TARJETA</span>
                                        <span class="tarjeta__value">${fn:escapeXml(physicalCard.maskedPan)}</span>
                                    </span>
                                    <span class="tarjeta__exp">
                                        <span class="tarjeta__key">VÁLIDA HASTA</span>
                                        <%--
                                          El marco escribe "05/27". La tabla card
                                          no guarda vencimiento —id, cuenta, tipo,
                                          PAN enmascarado, estado y alta—, así que
                                          va un guion en vez de una fecha inventada.
                                        --%>
                                        <span class="tarjeta__value">—</span>
                                    </span>
                                </span>
                            </label>
                        </c:if>

                        <c:if test="${not empty digitalCard}">
                            <label class="tarjeta tarjeta--b" for="front-b">
                                <span class="tarjeta__top">
                                    <span class="tarjeta__key">TIPO DE LA TARJETA</span>
                                    <span class="tarjeta__pill">DIGITAL</span>
                                </span>
                                <span class="tarjeta__data">
                                    <span class="tarjeta__pan">
                                        <span class="tarjeta__key">NÚMERO DE TARJETA</span>
                                        <span class="tarjeta__value">${fn:escapeXml(digitalCard.maskedPan)}</span>
                                    </span>
                                    <span class="tarjeta__exp">
                                        <span class="tarjeta__key">VÁLIDA HASTA</span>
                                        <span class="tarjeta__value">—</span>
                                    </span>
                                </span>
                            </label>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="paccounts__foot pcards-foot">
                <a class="pback" href="${ctx}/app/home">
                    <svg width="48" height="10" viewBox="0 0 48 10" aria-hidden="true">
                        <path d="M47 5H1M6 1 1 5l5 4" fill="none" stroke="currentColor"
                              stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span>Regresar a cuentas</span>
                </a>
            </div>
        </div>
    </section>

    <aside class="pactivity">
        <div class="pactivity__head">
            <p class="pactivity__title">ACTIVIDAD RECIENTE</p>
            <svg class="pactivity__filter" width="21" height="21" aria-hidden="true"><use href="#i-sliders"/></svg>
        </div>

        <c:choose>
            <c:when test="${empty activity}">
                <p class="paccounts__empty">Esta cuenta no registra movimientos.</p>
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

        <%-- Sin destino todavía: no existe la pantalla de historial del portal. --%>
        <span class="pactivity__more is-pending" title="Pantalla pendiente">Ver historial completo</span>
    </aside>
</div>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
