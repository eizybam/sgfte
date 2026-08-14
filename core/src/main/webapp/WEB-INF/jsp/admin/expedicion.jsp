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
                <span class="issue__label">TARJETAHABIENTE</span>
                <div class="issue__box">
                    <button type="button" class="issue__input picker__trigger" id="holderTrigger"
                            data-picker="cardholder"
                            data-picker-target="cardholder"
                            data-picker-title="Elegir tarjetahabiente"
                            data-picker-placeholder="Buscar por nombre, ID de empleado o correo">
                        <span id="holderLabel" class="picker__placeholder">Selecciona al tarjetahabiente</span>
                    </button>
                    <svg class="issue__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
                </div>
                <%-- No se envía al servidor: sólo decide qué cuentas se piden. --%>
                <input type="hidden" id="cardholderId" value="${selectedHolderId}">
            </div>

    <div class="issue__field">
        <label class="issue__label" for="accountId">CUENTA DESTINO</label>
        <div class="issue__box">
            <select class="issue__input" id="accountId" name="accountId" required
            ${empty selectedHolderId ? 'disabled' : ''}>
                <c:choose>
                    <c:when test="${empty targets}">
                        <option value="" disabled selected>Elige primero al tarjetahabiente</option>
                    </c:when>
                    <c:otherwise><jsp:include page="/WEB-INF/jsp/admin/picker-issue-options.jsp"/></c:otherwise>
                </c:choose>
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
        var trigger    = document.getElementById("holderTrigger");
        var holderId   = document.getElementById("cardholderId");
        var holderLbl  = document.getElementById("holderLabel");
        var accountSel = document.getElementById("accountId");
        var cardName   = document.getElementById("cardName");
        var ctx        = "${ctx}";

        function text(id, value) { document.getElementById(id).textContent = value; }

        function selectedType() {
            var checked = document.querySelector('input[name="cardType"]:checked');
            return checked && checked.value === "DIGITAL" ? "Digital" : "Física";
        }

        // ---- SIN CAMBIOS respecto a la versión anterior de esta pantalla ----
        // Sigue funcionando porque las <option> que llegan por fetch traen los
        // mismos data-* que traían las que escribía el c:forEach.
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
        // ---------------------------------------------------------------------

        /*
          Las cuentas del empleado elegido, pedidas al vuelo.

          Lo que llega es HTML: una ristra de <option> ya escapadas por JSTL. Va
          directo a innerHTML del <select>. No hay JSON que parsear ni <option>
          que construir a mano, y por eso paint() ni se entera.
        */
        function loadAccounts(id) {
            accountSel.disabled = true;
            accountSel.innerHTML = '<option value="" disabled selected>Cargando…</option>';
            paint();

            fetch(ctx + "/admin/picker?type=issue-options&cardholderId=" + encodeURIComponent(id),
                { credentials: "same-origin" })
                .then(function (res) {
                    if (res.redirected) { window.location.reload(); return null; }
                    if (!res.ok) throw new Error("HTTP " + res.status);
                    return res.text();
                })
                .then(function (html) {
                    if (html === null) return;
                    accountSel.innerHTML = html;
                    accountSel.disabled = false;
                    paint();
                })
                .catch(function () {
                    accountSel.innerHTML =
                        '<option value="" disabled selected>No se pudieron cargar las cuentas</option>';
                    paint();
                });
        }

        /*
          El selector no llama a esta pantalla: emite un evento y se olvida.
          Se comprueba `target` porque en otras pantallas hay más de un selector
          en la misma página y todos disparan el mismo evento.
        */
        document.addEventListener("picker:choose", function (e) {
            if (e.detail.target !== "cardholder") return;

            var d = e.detail.data;                       // los data-* del <tr>
            holderId.value = d.id;
            holderLbl.textContent = d.name + " · " + d.code;
            holderLbl.classList.remove("picker__placeholder");
            loadAccounts(d.id);
        });

        accountSel.addEventListener("change", paint);
        document.querySelectorAll('input[name="cardType"]').forEach(function (r) {
            r.addEventListener("change", paint);
        });

        paint();
    })();
</script>
<%@ include file="/WEB-INF/jsp/partials/picker-modal.jspf" %>
<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
