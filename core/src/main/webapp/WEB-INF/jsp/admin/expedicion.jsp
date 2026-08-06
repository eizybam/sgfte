<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Expedir Tarjeta · marco Figma "Expedir Tarjeta - Admin" (2038:129).

  El marco enseña la pantalla ya rellenada, sin decir de dónde sale cada dato.
  Aquí lo que se elige son DOS cosas: el tarjetahabiente y, dentro de él, la
  cuenta. Todo lo demás —nombre en la tarjeta, propósito, número de cuenta,
  saldo, vista previa y resumen— se deriva de la cuenta elegida y se actualiza
  en el navegador, sin ir al servidor.

  Los datos de cada cuenta viajan como atributos data-* en su propia <option>,
  no como JSON incrustado: así el escapado lo hace fn:escapeXml y no hay que
  confiar en que un nombre no traiga comillas.
--%>
<c:set var="pageTitle" value="Expedir Tarjeta"/>
<c:set var="pageSubtitle" value="Emite una tarjeta física o digital para una cuenta"/>
<c:set var="activeNav" value="cards"/>
<c:set var="bodyClass" value="has-topglow"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<form method="post" action="${ctx}/admin/cards">
    <input type="hidden" name="action" value="issue">

    <div class="issue-grid">

        <section class="issue-card">
            <p class="issue__section">DATOS DE LA TARJETA</p>

            <div class="issue__field">
                <label class="issue__label" for="cardholderId">TARJETAHABIENTE</label>
                <div class="issue__box">
                    <%-- No se envía: sólo filtra el desplegable de cuentas. --%>
                    <select class="issue__input" id="cardholderId" required>
                        <option value="" disabled selected>Selecciona al tarjetahabiente</option>
                        <c:forEach var="h" items="${holders}">
                            <option value="${h.key}">${fn:escapeXml(h.value)}</option>
                        </c:forEach>
                    </select>
                    <svg class="issue__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
                </div>
            </div>

            <div class="issue__field">
                <label class="issue__label" for="accountId">CUENTA DESTINO</label>
                <div class="issue__box">
                    <select class="issue__input" id="accountId" name="accountId" required>
                        <option value="" disabled selected>Selecciona la cuenta</option>
                        <c:forEach var="t" items="${targets}">
                            <option value="${t.accountId}"
                                    data-holder-id="${t.cardholderId}"
                                    data-holder="${fn:escapeXml(t.cardholderName)}"
                                    data-account="${fn:escapeXml(t.accountNumber)}"
                                    data-purpose="${fn:escapeXml(t.purpose)}"
                                    data-balance="$<fmt:formatNumber value="${t.balance}" type="number"
                                            groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/> MXN"
                                    ${t.accountId == selectedAccountId ? 'selected' : ''}>Cuenta ${fn:escapeXml(t.purpose)} · ${fn:escapeXml(t.accountNumber)}</option>
                        </c:forEach>
                    </select>
                    <svg class="issue__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
                </div>
            </div>

            <div class="issue__field">
                <label class="issue__label" for="cardName">NOMBRE EN LA TARJETA</label>
                <div class="issue__box">
                    <%--
                      Sólo lectura: la tabla `card` no guarda un nombre, así que
                      esto es lo que se imprimiría, derivado del tarjetahabiente.
                    --%>
                    <input class="issue__input" id="cardName" type="text" readonly tabindex="-1"
                           placeholder="—" value="">
                </div>
            </div>

            <div class="issue__field issue__field--type">
                <span class="issue__label">TIPO DE TARJETA</span>
                <div class="issue__types">
                    <label class="issue__type">
                        <input type="radio" name="cardType" value="PHYSICAL" checked>
                        Física
                    </label>
                    <label class="issue__type">
                        <input type="radio" name="cardType" value="DIGITAL">
                        Digital
                    </label>
                </div>
            </div>
        </section>

        <aside>
            <p class="issue__aside-label">VISTA PREVIA</p>

            <div class="card-preview">
                <p class="card-preview__brand">SGFTE</p>
                <p class="card-preview__type" id="pv-type">FÍSICA</p>
                <div class="card-preview__chip"></div>
                <%-- El PAN se genera al expedir, así que aún no hay 4 dígitos. --%>
                <p class="card-preview__pan">••••  ••••  ••••  ••••</p>
                <p class="card-preview__key card-preview__key--holder">TITULAR</p>
                <p class="card-preview__value card-preview__value--holder" id="pv-holder">—</p>
                <p class="card-preview__key card-preview__key--purpose">PROPÓSITO</p>
                <p class="card-preview__value card-preview__value--purpose" id="pv-purpose">—</p>
            </div>

            <div class="issue-summary">
                <p class="issue-summary__title">RESUMEN</p>
                <dl class="issue-summary__list">
                    <div class="issue-summary__row"><dt>Propósito</dt><dd id="sm-purpose">—</dd></div>
                    <div class="issue-summary__row"><dt>Cuenta</dt><dd id="sm-account">—</dd></div>
                    <div class="issue-summary__row"><dt>Tipo</dt><dd id="sm-type">Física</dd></div>
                    <div class="issue-summary__row"><dt>Saldo cuenta</dt><dd id="sm-balance">—</dd></div>
                </dl>
            </div>
        </aside>
    </div>

    <div class="issue-actions">
        <button type="submit" class="btn btn--primary btn--hero">
            <img src="${ctx}/assets/img/icons/transfer.png" alt="">
            Expedir tarjeta
        </button>
    </div>
</form>

<script>
    (function () {
        var holderSel  = document.getElementById("cardholderId");
        var accountSel = document.getElementById("accountId");
        var cardName   = document.getElementById("cardName");

        // Se guardan todas las <option> de cuenta para poder rearmar la lista
        // al cambiar de tarjetahabiente. Ocultar <option> no es fiable entre
        // navegadores; volver a insertarlas sí.
        var allAccounts = Array.prototype.slice.call(accountSel.options)
                               .filter(function (o) { return o.value !== ""; });
        var placeholder = accountSel.options[0];

        function text(id, value) { document.getElementById(id).textContent = value; }

        function selectedType() {
            var checked = document.querySelector('input[name="cardType"]:checked');
            return checked && checked.value === "DIGITAL" ? "Digital" : "Física";
        }

        function paint() {
            var opt = accountSel.selectedOptions[0];
            var has = opt && opt.value !== "";
            var type = selectedType();

            cardName.value = has ? opt.dataset.holder.toUpperCase() : "";

            text("pv-type", type.toUpperCase());
            text("pv-holder",  has ? opt.dataset.holder.toUpperCase() : "—");
            text("pv-purpose", has ? opt.dataset.purpose.toUpperCase() : "—");

            text("sm-purpose", has ? opt.dataset.purpose : "—");
            text("sm-account", has ? opt.dataset.account : "—");
            text("sm-type", type);
            text("sm-balance", has ? opt.dataset.balance : "—");
        }

        function fillAccounts(keepValue) {
            var holder = holderSel.value;
            accountSel.replaceChildren(placeholder);
            allAccounts
                .filter(function (o) { return o.dataset.holderId === holder; })
                .forEach(function (o) { accountSel.appendChild(o); });

            accountSel.value = keepValue || "";
            if (!accountSel.value) placeholder.selected = true;
            paint();
        }

        holderSel.addEventListener("change", function () { fillAccounts(null); });
        accountSel.addEventListener("change", paint);
        document.querySelectorAll('input[name="cardType"]').forEach(function (r) {
            r.addEventListener("change", paint);
        });

        // Al volver de expedir, la cuenta llega preseleccionada: se ajusta el
        // tarjetahabiente para que las dos listas queden coherentes.
        var preselected = accountSel.selectedOptions[0];
        if (preselected && preselected.value !== "") {
            holderSel.value = preselected.dataset.holderId;
            fillAccounts(preselected.value);
        } else {
            accountSel.replaceChildren(placeholder);
            paint();
        }
    })();
</script>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
