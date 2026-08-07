<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Mis tarjetas · todas las tarjetas del tarjetahabiente, en TODAS sus cuentas.

  A diferencia de la vista de una cuenta (263:58), aquí no hay "como mucho
  dos" — puede tener una tarjeta por cuenta y varias cuentas, así que en vez
  del solapado delante/detrás va una rejilla que se desplaza (mismo patrón de
  panel con cabecera y pie fijos que .paccounts en el dashboard).

  Cada tarjeta lleva encima su propia píldora de cuenta (propósito + número),
  con el color de la categoría — el mismo componente badge--pN que usa el
  admin — porque aquí, a diferencia de la vista de una cuenta, SÍ hace falta
  distinguir de qué cuenta es cada una.
--%>
<c:set var="pageTitle" value="Mis tarjetas"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<c:set var="cardCount" value="${fn:length(cards)}"/>

<header class="hist-head">
    <div>
        <h1 class="hist-title">Mis tarjetas</h1>
        <p class="hist-lead">Todas tus tarjetas, en todas tus cuentas, en un solo lugar.</p>
    </div>
</header>

<div class="paccounts tarjetas-panel">
    <div class="paccounts__head pcards-head">
        <h2>Tarjetas</h2>
        <c:if test="${cardCount gt 0}">
            <p class="pcards-hint">
                ${cardCount} ${cardCount eq 1 ? 'tarjeta activa' : 'tarjetas activas'} ·
                da click en cualquiera para ver su detalle.
            </p>
        </c:if>
    </div>

    <c:choose>
        <c:when test="${cardCount eq 0}">
            <p class="paccounts__empty">Todavía no tienes tarjetas. Administración las expide.</p>
        </c:when>
        <c:otherwise>
            <div class="paccounts__scroll">
                <div class="tarjetas-grid">
                    <c:forEach var="k" items="${cards}">
                        <div class="tarjetas-grid__item">
                            <%-- La cuenta a la que pertenece — lo único que
                                 esta pantalla añade sobre la cara de la
                                 tarjeta de siempre. --%>
                            <span class="badge badge--p${k.colorIndex} tarjetas-grid__badge">
                                ${fn:escapeXml(k.purpose)} · ${fn:escapeXml(k.accountNumber)}
                            </span>

                            <div class="tarjeta tarjeta--still tarjeta--pick" tabindex="0" role="button"
                                 aria-haspopup="dialog" data-card="card-detail-${k.id}">
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
            <span>Regresar al panel</span>
        </a>
    </div>
</div>

<%-- Un modal de detalle por tarjeta, mismo marco "Detalle Tarjeta" (285:47)
     que usa la vista de una cuenta — aquí cada una lleva SU PROPIA cuenta,
     no la de una sola pantalla. --%>
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
                            <span class="cardx__mono">${fn:escapeXml(k.accountNumber)}</span>
                            <span class="cardx__tag">${fn:escapeXml(k.purpose)}</span>
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

                <%-- Simula una compra real con esta tarjeta. Un solo modal
                     compartido (gasto-modal.jspf) lee la cuenta de aquí. --%>
                <button type="button" class="btn btn--secondary btn--hero btn--block" data-open-gasto
                        data-account="${k.accountId}" data-card="${k.id}">
                    Hacer un gasto
                </button>
            </div>
        </div>
    </div>
</c:forEach>

<%@ include file="/WEB-INF/jsp/partials/gasto-modal.jspf" %>

<script>
    (function () {
        // Cada tarjeta abre su propio detalle — a diferencia de la vista de
        // una cuenta, aquí no hay delante/detrás que intercambiar primero.
        function openCard(el) {
            var modal = document.getElementById(el.dataset.card);
            if (modal) modal.hidden = false;
        }

        document.querySelectorAll(".tarjetas-grid .tarjeta--pick").forEach(function (card) {
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

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
