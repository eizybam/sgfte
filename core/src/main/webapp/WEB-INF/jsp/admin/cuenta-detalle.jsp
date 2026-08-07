<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Detalle de Cuenta · marco Figma "Detalle de Cuenta - Admin" (2043:155).

  La pantalla es de sólo lectura: cada acción lleva a otro sitio. "Depositar"
  abre el mismo modal de dispersión de la Vista Global, ya fijado a esta cuenta
  y con un campo oculto para volver aquí en vez de al panel.

  Trae su propia cabecera (miga de pan, píldora de propósito y subtítulo), así
  que desactiva la del cascarón con hidePageHead.
--%>
<c:set var="pageTitle" value="Cuenta ${account.purpose}"/>
<c:set var="activeNav" value="accounts"/>
<c:set var="hidePageHead" value="true"/>
<c:set var="mainClass" value="app-main--flush"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<header class="detail-head">
    <p class="crumbs">
        <a href="${ctx}/admin/cuentas">Gestión de cuentas</a>
        <span class="crumbs__sep">›</span>
        Detalle de cuenta
    </p>

    <h1 class="detail-head__title">
        Cuenta ${fn:escapeXml(account.purpose)}
        <span class="purpose-pill purpose-pill--p${account.purposeColor}">${fn:escapeXml(account.purpose)}</span>
    </h1>

    <p class="detail-head__meta">
        ${fn:escapeXml(account.accountNumber)}
        <span class="crumbs__sep">·</span>
        Titular: ${fn:escapeXml(account.holderName)}
    </p>
</header>

<div class="detail-grid">

    <div>
        <section class="panel balance">
            <p class="panel__label">SALDO DISPONIBLE</p>

            <p class="balance__figure">
                <span>$<fmt:formatNumber value="${account.balance}" type="number"
                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/></span>
                <span class="balance__currency">MXN</span>
            </p>

            <%--
              El marco añade una descripción del propósito ("Combustible
              vehicular asignado"). La tabla `category` sólo guarda el nombre,
              así que aquí va únicamente el propósito real.
            --%>
            <p class="balance__purpose">
                <span class="balance__dot" style="background: var(--sgfte-purpose-${account.purposeColor});"></span>
                ${fn:escapeXml(account.purpose)}
            </p>

            <div class="balance__actions">
                <button type="button" class="btn btn--primary btn--hero" data-open-dispersion>Depositar</button>
                <a class="btn btn--secondary btn--hero" href="${ctx}/admin/transferencia">Transferir</a>
            </div>
        </section>

        <section class="panel moves" style="margin-top: 28px;">
            <div class="moves__head">
                <p class="panel__label">MOVIMIENTOS RECIENTES</p>
                <a class="moves__more" href="${ctx}/admin/historial?accountId=${account.id}">Ver historial completo</a>
            </div>

            <c:choose>
                <c:when test="${empty movements}">
                    <p class="moves__empty">Esta cuenta todavía no registra movimientos.</p>
                </c:when>
                <c:otherwise>
                    <table class="moves__table">
                        <thead>
                        <tr>
                            <th class="moves__col-date">FECHA</th>
                            <th>CONCEPTO</th>
                            <th class="moves__col-amount">MONTO</th>
                            <th class="moves__col-state">ESTADO</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="m" items="${movements}">
                            <tr>
                                <td class="moves__date">${m.dayLabel}</td>
                                <td class="moves__concept">${fn:escapeXml(m.concept)}</td>
                                <td class="moves__amount ${m.inflow ? 'moves__amount--in' : ''}">
                                    ${m.inflow ? '+' : '-'}$<fmt:formatNumber value="${m.amount}" type="number"
                                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/>
                                </td>
                                <%--
                                  Siempre COMPLETADO: el movimiento se escribe
                                  dentro de la transacción que ya movió el dinero
                                  y el ledger es inmutable, así que no existe un
                                  estado pendiente que mostrar.
                                --%>
                                <td class="moves__state"><span class="state-badge state-badge--done">COMPLETADO</span></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <div>
        <section class="panel linked">
            <p class="panel__label">TARJETAS VINCULADAS</p>

            <div class="linked__list">
                <c:forEach var="k" items="${cards}">
                    <div class="linked__card">
                        <span class="linked__chip"></span>
                        <span>
                            <span class="linked__type">${k.cardType == 'PHYSICAL' ? 'Física' : 'Digital'}</span>
                            <span class="linked__pan">••••&nbsp;&nbsp;${fn:substring(k.maskedPan, fn:length(k.maskedPan) - 4, fn:length(k.maskedPan))}</span>
                        </span>
                        <span class="linked__state ${k.status == 'ACTIVE' ? '' : 'linked__state--off'}"
                              title="${k.status == 'ACTIVE' ? 'Activa' : 'Inactiva'}"></span>
                    </div>
                </c:forEach>
                <c:if test="${empty cards}">
                    <p class="moves__empty" style="padding: var(--sp-3) 0;">Sin tarjetas vinculadas.</p>
                </c:if>
            </div>

            <a class="linked__add" href="${ctx}/admin/cards?accountId=${account.id}">+&nbsp;&nbsp;Expedir nueva tarjeta</a>
        </section>

        <section class="panel month" style="margin-top: 28px;">
            <p class="panel__label">RESUMEN DEL MES</p>
            <dl class="month__list">
                <div class="month__row">
                    <dt>Dispersado</dt>
                    <dd>$<fmt:formatNumber value="${summary.dispersed}" type="number"
                            groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/> MXN</dd>
                </div>
                <div class="month__row">
                    <dt>Movimientos</dt>
                    <dd>${summary.movements}</dd>
                </div>
                <div class="month__row">
                    <dt>Última recarga</dt>
                    <dd>${empty summary.lastDeposit ? '—' : lastDepositLabel}</dd>
                </div>
                <%--
                  El marco muestra un "Límite mensual". No existe tal columna en
                  el esquema —ni por cuenta ni por categoría—, así que se deja el
                  guion en vez de inventar una cifra.
                --%>
                <div class="month__row">
                    <dt>Límite mensual</dt>
                    <dd>—</dd>
                </div>
                <div class="month__row">
                    <dt>Tarjetas activas</dt>
                    <dd>${summary.activeCards}</dd>
                </div>
            </dl>
        </section>
    </div>
</div>

<%-- Mismo modal que la Vista Global, fijado a esta cuenta. --%>
<c:set var="dispersionFailed" value="${not empty dispersionErrors}"/>

<div class="modal-scrim" id="dispersion-modal" ${dispersionFailed ? '' : 'hidden'}>
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="dispersion-title">
        <h2 class="modal__title" id="dispersion-title">Dispersión de fondos</h2>
        <div class="modal__rule"></div>

        <c:if test="${dispersionFailed}">
            <div class="alert alert--error modal__alert">
                <ul><c:forEach var="e" items="${dispersionErrors}"><li>${e}</li></c:forEach></ul>
            </div>
        </c:if>

        <form class="modal__body" method="post" action="${ctx}/admin/dispersion">
            <input type="hidden" name="accountId" value="${account.id}">
            <input type="hidden" name="returnToAccount" value="${account.id}">

            <div class="modal__field">
                <span class="modal__label">CUENTA DESTINO · Origen: Concentradora</span>
                <div class="modal__control modal__control--select">
                    <svg class="modal__icon-card" width="16" height="12" aria-hidden="true"><use href="#i-card-slot"/></svg>
                    <span class="modal__input" style="display:flex; align-items:center;">
                        Cuenta ${fn:escapeXml(account.purpose)} · ${fn:escapeXml(account.accountNumber)}
                    </span>
                </div>
            </div>

            <div class="modal__field">
                <label class="modal__label modal__label--tracked" for="amount">MONTO</label>
                <div class="modal__control modal__control--amount">
                    <svg class="modal__icon-cash" width="16.74" height="17" aria-hidden="true"><use href="#i-cash-app"/></svg>
                    <input class="modal__input" type="number" step="0.01" min="0.01"
                           id="amount" name="amount" placeholder="0.00"
                           value="${fn:escapeXml(dispersionAmount)}" required>
                </div>
            </div>

            <div class="modal__actions">
                <button type="button" class="btn btn--secondary btn--hero" data-close-dispersion>Cancelar</button>
                <button type="submit" class="btn btn--primary btn--hero">
                    <img src="${ctx}/assets/img/icons/disperse.png" alt="">
                    Dispersar
                </button>
            </div>
        </form>
    </div>
</div>

<script>
    (function () {
        var scrim = document.getElementById("dispersion-modal");
        var amount = document.getElementById("amount");
        var lastFocused = null;

        function open() {
            lastFocused = document.activeElement;
            scrim.hidden = false;
            amount.focus();
        }

        function close() {
            scrim.hidden = true;
            if (lastFocused) lastFocused.focus();
        }

        document.querySelectorAll("[data-open-dispersion]").forEach(function (b) {
            b.addEventListener("click", open);
        });
        document.querySelectorAll("[data-close-dispersion]").forEach(function (b) {
            b.addEventListener("click", close);
        });

        scrim.addEventListener("mousedown", function (e) { if (e.target === scrim) close(); });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && !scrim.hidden) close();
        });

        if (!scrim.hidden) amount.focus();
    })();
</script>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
