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

<c:set var="cardCount" value="${fn:length(cards)}"/>

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
        <%-- Mis tarjetas navega; Soporte y Políticas abren cada uno una
             ventana informativa simple (info-modals.jspf), igual que en el
             dashboard. --%>
        <nav class="pquick">
            <a class="pquick__item" href="${ctx}/app/tarjetas">
                <svg width="30" height="32" aria-hidden="true"><use href="#i-cards"/></svg>
                <span>Mis tarjetas</span>
            </a>
            <span class="pquick__item" role="button" tabindex="0" data-open-soporte>
                <svg width="32" height="32" aria-hidden="true"><use href="#i-headset"/></svg>
                <span>Soporte</span>
            </span>
            <span class="pquick__item" role="button" tabindex="0" data-open-politicas>
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
            <button type="button" class="btn btn--primary btn--hero btn--stacked" data-open-transfer>
                <svg width="24" height="24" aria-hidden="true"><use href="#i-arrows"/></svg>
                <span>Transferir<br>a compañero</span>
            </button>
        </div>

        <div class="paccounts pcards-panel">
            <div class="paccounts__head pcards-head">
                <h2>Tarjetas</h2>
                <c:if test="${cardCount gt 0}">
                    <p class="pcards-hint">
                        ${cardCount} ${cardCount eq 1 ? 'tarjeta' : 'tarjetas'} ·
                        da click en cualquiera para ver su detalle.
                    </p>
                </c:if>
            </div>

            <%--
              Las tarjetas de la cuenta, en rejilla y con desplazamiento.

              Antes eran DOS solapadas en posición absoluta, con un par de radios
              para traer al frente la de atrás. Eso venía del marco, y funcionaba
              mientras la pantalla enseñara exactamente dos: con una sola la
              tarjeta se estiraba a todo el ancho —dejaba de parecer una tarjeta—
              y debajo quedaba el hueco de la segunda; con tres no había dónde
              ponerlas, así que el servlet recortaba a dos y de paso escondía las
              bloqueadas.

              Ahora es la misma rejilla que "Mis tarjetas": fluye, se desplaza
              cuando no cabe y cada tarjeta conserva su tamaño real. Lo que se
              pierde es el efecto de baraja del marco; lo que se gana es que la
              pantalla enseña lo que la cuenta TIENE en vez de lo que cabía.
            --%>
            <c:choose>
                <c:when test="${cardCount eq 0}">
                    <p class="paccounts__empty">
                        Esta cuenta todavía no tiene tarjetas. Administración las expide.
                    </p>
                </c:when>
                <c:otherwise>
                    <div class="paccounts__scroll pcards-scroll">
                        <div class="pcards">
                            <c:forEach var="k" items="${cards}">
                                <div class="tarjeta tarjeta--still tarjeta--pick ${k.status == 'BLOCKED' ? 'tarjeta--blocked' : ''}"
                                     tabindex="0" role="button" aria-haspopup="dialog"
                                     data-card="card-detail-${k.id}">
                                    <span class="tarjeta__top">
                                        <span class="tarjeta__key">TIPO DE LA TARJETA</span>
                                        <span class="tarjeta__pill">${fn:toUpperCase(k.typeLabel)}</span>
                                    </span>
                                    <span class="tarjeta__data">
                                        <span class="tarjeta__pan">
                                            <span class="tarjeta__key">NÚMERO DE TARJETA</span>
                                            <span class="tarjeta__value">${fn:escapeXml(k.maskedPan)}</span>
                                        </span>
                                        <span class="tarjeta__exp">
                                            <span class="tarjeta__key">VÁLIDA HASTA</span>
                                            <span class="tarjeta__value">${k.expiresLabel}</span>
                                        </span>
                                    </span>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>

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

        <%-- /app/movimientos existe y acepta ?account=: no hacía falta pantalla
             nueva, sólo apuntar el enlace a la que ya estaba hecha. --%>
        <a class="pactivity__more" href="${ctx}/app/movimientos?account=${account.id}">Ver historial completo</a>
    </aside>
</div>

<%--
  Detalle de tarjeta · marco Figma "Detalle Tarjeta" (285:47).

  Uno por tarjeta, porque son como mucho dos: repetir el bloque cuesta menos
  que rellenar un modal único desde JavaScript, y así el contenido lo pinta el
  servidor como en el resto del proyecto.

  Al pulsar la tarjeta de DETRÁS se intercambian —que es lo que ya hacían los
  radios—; al pulsar la de DELANTE, que ya enseña su cara, se abre este detalle.
  Es lo que dice la pantalla: "da click en cualquier tarjeta para ver sus
  detalles completos", y el detalle tiene más de lo que cabe en la cara.
--%>
<c:forEach var="k" items="${cards}">
    <div class="modal-scrim" id="card-detail-${k.id}" hidden>
        <div class="modal cardx" role="dialog" aria-modal="true" aria-labelledby="cardx-title-${k.id}">
            <div class="cardx__head">
                <h2 class="cardx__title" id="cardx-title-${k.id}">Detalles de tarjeta</h2>
                <button type="button" class="cardx__close" data-close-card aria-label="Cerrar">
                    <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M6 6l12 12M18 6L6 18" fill="none" stroke="currentColor"
                              stroke-width="2" stroke-linecap="round"/>
                    </svg>
                </button>
            </div>

            <div class="cardx__body">
                <%-- La misma cara de tarjeta de la pantalla, en su tamaño grande. --%>
                <div class="tarjeta tarjeta--still">
                    <span class="tarjeta__top">
                        <span class="tarjeta__key">TIPO DE LA TARJETA</span>
                        <span class="tarjeta__pill">${fn:toUpperCase(k.typeLabel)}</span>
                    </span>
                    <span class="tarjeta__data">
                        <span class="tarjeta__pan">
                            <span class="tarjeta__key">NÚMERO DE TARJETA</span>
                            <span class="tarjeta__value">${fn:escapeXml(k.maskedPan)}</span>
                        </span>
                        <span class="tarjeta__exp">
                            <span class="tarjeta__key">VÁLIDA HASTA</span>
                            <span class="tarjeta__value">${k.expiresLabel}</span>
                        </span>
                    </span>
                </div>

                <dl class="cardx__rows">
                    <div class="cardx__row">
                        <dt>Cuenta asociada</dt>
                        <dd>
                            <span class="cardx__mono">${fn:escapeXml(account.accountNumber)}</span>
                            <span class="cardx__tag">${fn:escapeXml(account.purpose)}</span>
                        </dd>
                    </div>
                    <div class="cardx__row">
                        <dt>Tipo de tarjeta</dt>
                        <dd><span class="cardx__mono">${k.typeLabel}</span></dd>
                    </div>
                    <div class="cardx__row">
                        <dt>Fecha de emisión</dt>
                        <dd><span class="cardx__mono">${k.issuedLabel}</span></dd>
                    </div>
                    <div class="cardx__row">
                        <dt>Fecha de expiración</dt>
                        <dd><span class="cardx__mono">${k.expiresLabel}</span></dd>
                    </div>
                </dl>
            </div>
        </div>
    </div>
</c:forEach>

<script>
    (function () {
        /*
          Cada tarjeta abre su detalle, sin más. Ya no hay delante y detrás que
          intercambiar primero: la rejilla las enseña todas a la vez, igual que
          en Mis tarjetas.
        */
        function openCard(el) {
            var modal = document.getElementById(el.dataset.card);
            if (modal) modal.hidden = false;
        }

        document.querySelectorAll(".pcards .tarjeta--pick").forEach(function (card) {
            card.addEventListener("click", function () { openCard(card); });
            card.addEventListener("keydown", function (e) {
                if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openCard(card); }
            });
        });

        function closeAll() {
            document.querySelectorAll("[id^='card-detail-']").forEach(function (m) { m.hidden = true; });
        }

        document.querySelectorAll("[data-close-card]").forEach(function (b) {
            b.addEventListener("click", closeAll);
        });
        document.querySelectorAll("[id^='card-detail-']").forEach(function (m) {
            m.addEventListener("mousedown", function (e) { if (e.target === m) closeAll(); });
        });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape") closeAll();
        });
    })();
</script>

<%-- Se abre con esta cuenta ya elegida como origen. --%>
<c:set var="fixedSourceId" value="${account.id}"/>
<%@ include file="/WEB-INF/jsp/partials/transfer-modal.jspf" %>
<%@ include file="/WEB-INF/jsp/partials/info-modals.jspf" %>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
