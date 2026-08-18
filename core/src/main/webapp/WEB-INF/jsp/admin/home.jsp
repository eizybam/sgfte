<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Vista Global · marco Figma "Vista principal de Admin" (201:22).

  La pantalla tiene tres piezas: la concentradora (fuente única de fondos,
  RN-02), dos KPIs y el reparto del dinero por propósito.

  Los importes se formatean con minFractionDigits=0 y maxFractionDigits=2, que
  es justo lo que hace el prototipo: 4250000 se ve "$4,250,000" y 853567.31 se
  ve "$853,567.31". No se redondea nada.
--%>
<c:set var="pageTitle" value="Vista Global"/>
<c:set var="pageSubtitle" value="Resumen financiero y control de dispersión"/>
<c:set var="activeNav" value="overview"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="vista-grid">

    <section class="conc">
        <p class="conc__label">CUENTA CONCENTRADORA</p>

        <p class="conc__amount">
            <span class="conc__figure">$<fmt:formatNumber value="${concentratorBalance}"
                    type="number" groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/></span>
            <span class="conc__currency">MXN</span>
        </p>

        <%--
          Aquí había un botón "Fondear" que abría un modal con un campo de
          monto. Ya no: desde V12 el saldo sólo sube cuando el banco reporta un
          depósito con referencia verificable, así que no hay nada que teclear.
          Fondear es transferir a la CLABE de la Concentradora y esperar; el
          detalle vive en su pantalla.
        --%>
        <div class="conc__cta">
            <button type="button" class="btn btn--primary btn--hero" data-open-dispersion>
                <img src="${ctx}/assets/img/icons/transfer.png" alt="">
                Depositar a cuenta
            </button>
            <a class="btn btn--secondary btn--hero" href="${ctx}/admin/concentradora">
                <img src="${ctx}/assets/img/icons/fund.png" alt="">
                Cómo fondear
            </a>
        </div>
    </section>

    <div class="kpi-col">
        <article class="kpi-card">
            <div class="kpi-card__head">
                <span class="kpi-card__label">Tarjetahabientes activos</span>
                <img class="kpi-card__icon" src="${ctx}/assets/img/icons/users.png" alt="">
            </div>
            <p class="kpi-card__value">${activeCardholders}</p>
        </article>

        <article class="kpi-card">
            <div class="kpi-card__head">
                <span class="kpi-card__label">Dispersión mensual total</span>
                <img class="kpi-card__icon" src="${ctx}/assets/img/icons/cash.png" alt="">
            </div>
            <p class="kpi-card__value">
                <span>$<fmt:formatNumber value="${dispersionThisMonth}"
                        type="number" groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/></span>
                <span class="kpi-card__currency">MXN</span>
            </p>
        </article>
    </div>
</div>

<section class="dist">
    <h2 class="dist__title">Distribución de gasto</h2>
    <p class="dist__lead">Análisis del propósito de fondos<br>asignados en el periodo actual.</p>

    <c:choose>
        <c:when test="${empty purposes}">
            <p class="dist__empty">Todavía no hay fondos asignados a ninguna cuenta.</p>
        </c:when>
        <c:otherwise>
            <%--
              Paradas del degradado del pastel. Se acumulan los porcentajes ya
              redondeados en el servlet; la última rebanada se cierra en 100%
              para que un redondeo de 99 o 101 no deje un hueco ni se solape.
            --%>
            <c:set var="acc" value="0"/>
            <c:set var="pieStops"><c:forEach var="p" items="${purposes}" varStatus="s"><c:if test="${not s.first}">,</c:if>var(--sgfte-purpose-${p.colorIndex}) ${acc}% ${s.last ? 100 : acc + p.percent}%<c:set var="acc" value="${acc + p.percent}"/></c:forEach></c:set>

            <ul class="dist__legend">
                <c:forEach var="p" items="${purposes}">
                    <li class="dist__item">
                        <span class="dist__key">
                            <span class="dist__swatch"
                                  style="background: var(--sgfte-purpose-${p.colorIndex});"></span>
                            <span class="dist__name">${p.purpose}</span>
                        </span>
                        <span class="dist__pct">${p.percent}%</span>
                    </li>
                </c:forEach>
            </ul>

            <div class="dist__pie" role="img"
                 aria-label="Reparto del saldo por propósito"
                 style="background: conic-gradient(${pieStops});"></div>
        </c:otherwise>
    </c:choose>
</section>

<%--
  Modal "Dispersión de fondos" (Figma 2177:376). Antes era la página
  /admin/dispersion; ahora esa ruta sólo redirige aquí.

  Se pinta siempre en el HTML y se muestra u oculta con [hidden]: el formulario
  no depende de JavaScript para existir, sólo para abrirse. Si la dispersión
  falló, el servlet dejó los errores en sesión y el modal arranca abierto con lo
  que se había tecleado.
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

            <div class="modal__field">
                <span class="modal__label">CUENTA DESTINO</span>
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
                            data-picker-placeholder="Buscar por cuenta, titular, correo o ID de empleado">
            <span id="accountLabel" class="${empty dispersionAccountLabel ? 'picker__placeholder' : ''}">
                ${empty dispersionAccountLabel
                        ? 'Cuenta a fondear'
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

<script>
    // El navegador no valida un input[type=hidden] aunque lleve required, así
    // que el "elige una cuenta" se hace aquí. Es una cortesía: quien decide de
    // verdad sigue siendo DispersionService, que rechaza un accountId nulo.
    document.querySelector('#dispersion-modal form').addEventListener("submit", function (e) {
        if (!document.getElementById("accountId").value) {
            e.preventDefault();
            document.getElementById("accountTrigger").focus();
        }
    });

    (function () {
        var scrim = document.getElementById("dispersion-modal");
        var firstField = document.getElementById("accountId");
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

        document.querySelectorAll("[data-open-dispersion]").forEach(function (b) {
            b.addEventListener("click", open);
        });
        document.querySelectorAll("[data-close-dispersion]").forEach(function (b) {
            b.addEventListener("click", close);
        });

        // Clic en el velo, pero no dentro del panel.
        scrim.addEventListener("mousedown", function (e) {
            if (e.target === scrim) close();
        });

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && !scrim.hidden) close();
        });

        // Si viene de un intento fallido, arranca abierto y con el foco puesto.
        if (!scrim.hidden) firstField.focus();
    })();
</script>


<%--
  Modal "Fondear Concentradora" (Figma 2177:344). Mismo componente que el de
  dispersión: sólo cambian el título, el primer campo y la etiqueta del botón.

  El método de fondeo se guarda en la bitácora, no en el ledger: es contexto
  operativo. El ledger guarda importe y saldo resultante, que es lo que cuadra.
--%>
<script>

    // El selector avisa; la pantalla decide. Se filtra por target porque el
    // evento es global y podría haber más de un selector en la página.
    document.addEventListener("picker:choose", function (e) {
        if (e.detail.target !== "dispersion-account") return;

        var d = e.detail.data;
        document.getElementById("accountId").value = d.id;
        var label = document.getElementById("accountLabel");
        label.textContent = d.holder + " — " + d.purpose + " · " + d.number;
        label.classList.remove("picker__placeholder");
    });
</script>

<%@ include file="/WEB-INF/jsp/partials/picker-modal.jspf" %>
<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
