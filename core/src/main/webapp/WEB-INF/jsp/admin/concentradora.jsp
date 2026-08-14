<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Cuenta Concentradora · marco Figma "Cuenta Concentradora - Admin" (2097:313).

  Es la misma rejilla que Detalle de Cuenta (660 + 340), así que reutiliza sus
  paneles tal cual: .balance, .moves, .linked y .month. Las tarjetas de
  reintegración del marco son idénticas a las de "Tarjetas vinculadas" —mismo
  chip de 46x30, mismo punto de estado—, de modo que no hacen falta estilos
  nuevos para ese panel.

  Trae su propia cabecera, así que desactiva la del cascarón con hidePageHead.
--%>
<c:set var="pageTitle" value="Cuenta Concentradora"/>
<%--
  El marco subraya "Dashboard" porque se dibujó cuando la Concentradora no
  tenía sitio propio en la cabecera. Ahora lo tiene, así que se marca ella.
--%>
<c:set var="activeNav" value="concentrator"/>
<c:set var="hidePageHead" value="true"/>
<c:set var="mainClass" value="app-main--flush"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<header class="detail-head">
    <p class="crumbs">
        <a href="${ctx}/admin/cuentas">Gestión de cuentas</a>
        <span class="crumbs__sep">›</span>
        Cuenta Concentradora
    </p>

    <h1 class="detail-head__title">Cuenta Concentradora</h1>

    <%--
      El marco escribe "CONC-001". No hay columna de código en
      concentrator_account —es una fila única—, así que se compone con su id en
      vez de teclear un literal que podría no corresponder con la base.
    --%>
    <p class="detail-head__meta">
        CONC-<fmt:formatNumber value="${concentrator.id}" minIntegerDigits="3" groupingUsed="false"/>
        <span class="crumbs__sep">·</span>
        Fuente única de dispersión
    </p>
</header>

<div class="detail-grid">

    <div>
        <section class="panel balance">
            <p class="panel__label">SALDO TOTAL DISPONIBLE</p>

            <p class="balance__figure">
                <span>$<fmt:formatNumber value="${concentrator.balance}" type="number"
                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/></span>
                <span class="balance__currency">MXN</span>
            </p>

            <p class="balance__purpose">
                <span class="balance__dot" style="background: var(--sgfte-primary);"></span>
                Cuenta central de fondeo · Origen de toda dispersión
            </p>

            <div class="balance__actions balance__actions--conc">
                <button type="button" class="btn btn--primary" data-open-fund>Fondear</button>
                <button type="button" class="btn btn--secondary" data-open-dispersion>Dispersar</button>
            </div>
        </section>

        <section class="panel moves" style="margin-top: 28px;">
            <div class="moves__head">
                <p class="panel__label">MOVIMIENTOS RECIENTES</p>
                <%--
                  Sin destino todavía: no existe una vista paginada del ledger de
                  la Concentradora. /admin/logs?mod=FONDOS parecería servir, pero
                  la bitácora es otra cosa —quién hizo qué, no el saldo—, así que
                  enlazarla mostraría lo que no es. Se deja inerte, como el botón
                  de exportar de Logs y Analíticas.
                --%>
                <span class="moves__more is-pending" title="Historial completo del ledger pendiente">Ver historial completo</span>
            </div>

            <c:choose>
                <c:when test="${empty movements}">
                    <p class="moves__empty">La Concentradora todavía no registra movimientos.</p>
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
                                  El marco dibuja un "PENDIENTE" en el fondeo. No
                                  existe tal estado: la fila del ledger se escribe
                                  dentro de la misma transacción que ya movió el
                                  saldo, y un disparador la vuelve inmutable. Si
                                  está en la tabla, ya ocurrió.
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
            <p class="panel__label">REINTEGRACIONES RECIENTES</p>

            <div class="linked__list">
                <c:forEach var="r" items="${reintegrations}">
                    <div class="linked__card">
                        <span class="linked__chip"></span>
                        <span>
                            <%--
                              El marco distingue el motivo ("Cuenta cerrada" /
                              "Usuario eliminado"). El ledger sólo guarda que
                              hubo una reintegración, así que se muestra la fecha,
                              que es el dato que sí existe.
                            --%>
                            <span class="linked__type">Reintegración</span>
                            <span class="linked__pan">+$<fmt:formatNumber value="${r.amount}" type="number"
                                    groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/>
                                · ${r.dayLabel}</span>
                        </span>
                        <span class="linked__state" title="Aplicada"></span>
                    </div>
                </c:forEach>
                <c:if test="${empty reintegrations}">
                    <p class="moves__empty" style="padding: var(--sp-3) 0;">Sin reintegraciones registradas.</p>
                </c:if>
            </div>

            <span class="linked__add is-pending" title="Listado completo de reintegraciones pendiente">Ver todas las reintegraciones</span>
        </section>

        <section class="panel month" style="margin-top: 28px;">
            <p class="panel__label">RESUMEN CONCENTRADORA</p>
            <dl class="month__list">
                <div class="month__row">
                    <dt>Dispersado (mes)</dt>
                    <dd>$<fmt:formatNumber value="${summary.dispersedThisMonth}" type="number"
                            groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/> MXN</dd>
                </div>
                <div class="month__row">
                    <dt>Movimientos</dt>
                    <dd>${summary.movementsThisMonth}</dd>
                </div>
                <div class="month__row">
                    <dt>Última reintegración</dt>
                    <dd>${empty lastReintegrationLabel ? '—' : lastReintegrationLabel}</dd>
                </div>
                <div class="month__row">
                    <dt>Reintegrado (mes)</dt>
                    <dd>$<fmt:formatNumber value="${summary.reintegratedThisMonth}" type="number"
                            groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/></dd>
                </div>
                <div class="month__row">
                    <dt>Cuentas activas</dt>
                    <dd>${activeAccounts}</dd>
                </div>
            </dl>
        </section>
    </div>
</div>

<%--
  Los dos modales son los mismos de la Vista Global; el campo oculto returnTo
  es lo único que cambia, para volver aquí en vez de al panel.
--%>
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
            <input type="hidden" name="returnTo" value="concentradora">

            <div class="modal__field">
                <span class="modal__label">CUENTA DESTINO · Origen: Concentradora</span>
                <div class="modal__control modal__control--select">
                    <svg class="modal__icon-card" width="16" height="12" aria-hidden="true"><use href="#i-card-slot"/></svg>
                    <%--
                      Antes: un <select> con todas las cuentas activas de la empresa, donde
                      dos "Gómez, Carlos" salían idénticos. Ahora un botón que abre el
                      selector con tabla — número de cuenta, titular, propósito y saldo, con
                      buscador.

                      Mismo aspecto que el campo fijo de cuenta-detalle.jsp: ahí la cuenta ya
                      se sabe y es un <span>; aquí se elige y es un <button>. La caja es la
                      misma en las dos.
                    --%>
                    <button type="button" class="modal__input picker__trigger" id="accountTrigger"
                            data-picker="account"
                            data-picker-target="dispersion-account"
                            data-picker-title="Elegir cuenta destino"
                            data-picker-placeholder="Buscar por número de cuenta, titular o ID de empleado">
            <span id="accountLabel" class="${empty dispersionAccountLabel ? 'picker__placeholder' : ''}">
                ${empty dispersionAccountLabel
                        ? 'Selecciona la cuenta a fondear'
                        : fn:escapeXml(dispersionAccountLabel)}
            </span>
                    </button>
                    <svg class="modal__icon-chev" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
                </div>
                <%-- Esto es lo que viaja al servidor, igual que viajaba el value del select. --%>
                <input type="hidden" id="accountId" name="accountId" value="${dispersionAccountId}" required>
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

<c:set var="fundFailed" value="${not empty fundErrors}"/>

<div class="modal-scrim" id="fund-modal" ${fundFailed ? '' : 'hidden'}>
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="fund-title">
        <h2 class="modal__title" id="fund-title">Fondear Concentradora</h2>
        <div class="modal__rule"></div>

        <c:if test="${fundFailed}">
            <div class="alert alert--error modal__alert">
                <ul><c:forEach var="e" items="${fundErrors}"><li>${e}</li></c:forEach></ul>
            </div>
        </c:if>

        <form class="modal__body" method="post" action="${ctx}/admin/concentradora">
            <input type="hidden" name="returnTo" value="concentradora">

            <div class="modal__field">
                <label class="modal__label" for="method">MÉTODO DE FONDEO</label>
                <div class="modal__control modal__control--select">
                    <svg class="modal__icon-card" width="16" height="12" aria-hidden="true"><use href="#i-card-slot"/></svg>
                    <select class="modal__input" id="method" name="method">
                        <option value="SPEI / Depósito bancario" selected>SPEI / Depósito bancario</option>
                        <option value="Transferencia interbancaria">Transferencia interbancaria</option>
                        <option value="Efectivo">Efectivo</option>
                    </select>
                    <svg class="modal__icon-chev" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
                </div>
            </div>

            <div class="modal__field">
                <label class="modal__label modal__label--tracked" for="fundAmount">MONTO</label>
                <div class="modal__control modal__control--amount">
                    <svg class="modal__icon-cash" width="16.74" height="17" aria-hidden="true"><use href="#i-cash-app"/></svg>
                    <input class="modal__input" type="number" step="0.01" min="0.01"
                           id="fundAmount" name="amount" placeholder="0.00"
                           value="${fn:escapeXml(fundAmount)}" required>
                </div>
            </div>

            <div class="modal__actions">
                <button type="button" class="btn btn--secondary btn--hero" data-close-fund>Cancelar</button>
                <button type="submit" class="btn btn--primary btn--hero">
                    <img src="${ctx}/assets/img/icons/disperse.png" alt="">
                    Confirmar
                </button>
            </div>
        </form>
    </div>
</div>

<script>
    // Un solo cableado para los dos modales: sólo cambian el velo, el disparador
    // y el campo que recibe el foco.
    (function () {
        function wire(scrimId, openAttr, closeAttr, firstFieldId) {
            var scrim = document.getElementById(scrimId);
            var firstField = document.getElementById(firstFieldId);
            var lastFocused = null;

            function open() {
                lastFocused = document.activeElement;
                scrim.hidden = false;
                firstField.focus();
            }

            function close() {
                scrim.hidden = true;
                if (lastFocused) lastFocused.focus();
            }

            document.querySelectorAll("[" + openAttr + "]").forEach(function (b) {
                b.addEventListener("click", open);
            });
            document.querySelectorAll("[" + closeAttr + "]").forEach(function (b) {
                b.addEventListener("click", close);
            });

            // Clic en el velo, pero no dentro del panel.
            scrim.addEventListener("mousedown", function (e) { if (e.target === scrim) close(); });
            document.addEventListener("keydown", function (e) {
                if (e.key === "Escape" && !scrim.hidden) close();
            });

            // Si viene de un intento fallido, arranca abierto y con el foco puesto.
            if (!scrim.hidden) firstField.focus();
        }

        wire("dispersion-modal", "data-open-dispersion", "data-close-dispersion", "accountId");
        wire("fund-modal", "data-open-fund", "data-close-fund", "fundAmount");
    })();
</script>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
