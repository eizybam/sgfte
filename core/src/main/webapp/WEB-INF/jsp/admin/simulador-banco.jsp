<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Simulador de banco · NO forma parte del sistema.

  Esta pantalla ocupa el lugar del banco: captura lo que en producción llegaría
  solo, cuando el PSP avisa que entró una transferencia. Se borra al desplegar
  de verdad y el webhook pega contra el mismo /api/banco/deposito sin que cambie
  nada del núcleo.

  Se anuncia como simulador en grande y a propósito. Lo que se defiende no es
  "es imposible inventarse un depósito" —con esta pantalla abierta, claro que se
  puede— sino que el sistema únicamente acepta dinero como un hecho externo con
  referencia verificable, y que sustituir el simulador por un banco real no
  obliga a tocar el servicio, el ledger ni las reglas.

  Reutiliza las clases del formulario de "Expedir Tarjeta" (.issue-card,
  .issue__field, .issue__box…): es el mismo tipo de formulario de página
  completa y no hacía falta CSS nuevo.
--%>
<c:set var="pageTitle" value="Simulador de banco"/>
<c:set var="pageSubtitle" value="Publica una notificación de depósito como lo haría el banco"/>
<c:set var="activeNav" value="movements"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<c:set var="f" value="${simForm}"/>

<div class="sim-warning">
    <strong>Esto no es parte del sistema.</strong>
    Sustituye al banco: en producción esta pantalla no existe y es el PSP quien
    publica en <code>/api/banco/deposito</code>. El formulario firma con HMAC y
    publica contra ese mismo endpoint, así que lo que se prueba aquí es el
    camino real, firma incluida.
    <c:if test="${secretoPorDefecto}">
        <br><strong>Además:</strong> se está usando el secreto de desarrollo.
        Define <code>SGFTE_BANK_SECRET</code> en el <code>.env</code> antes de desplegar.
    </c:if>
</div>

<c:if test="${not empty simOk}">
    <div class="alert alert--ok">Depósito aplicado · ${fn:escapeXml(simOk)}</div>
</c:if>
<c:if test="${not empty simError}">
    <div class="alert alert--error">Depósito rechazado · ${fn:escapeXml(simError)}</div>
</c:if>

<div class="issue-grid">
    <form class="issue-card" method="post" action="${ctx}/admin/simulador-banco" id="sim-form">

        <p class="issue__section">Cómo llegó el dinero</p>

        <%-- El canal manda: cambia qué campos son obligatorios y cuáles no
             existen. La regla de verdad está en FundingService y en los CHECK
             de la base; esto sólo evita teclear lo que va a ser rechazado. --%>
        <div class="issue__field issue__field--type">
            <span class="issue__label">CANAL</span>
            <div class="issue__types">
                <label class="issue__type">
                    <input type="radio" name="canal" value="SPEI"
                           ${f.canal == 'VENTANILLA' ? '' : 'checked'}>
                    <span>Transferencia SPEI</span>
                </label>
                <label class="issue__type">
                    <input type="radio" name="canal" value="VENTANILLA"
                           ${f.canal == 'VENTANILLA' ? 'checked' : ''}>
                    <span>Efectivo en ventanilla</span>
                </label>
            </div>
        </div>

        <div class="issue__field">
            <label class="issue__label" for="referencia">
                <span data-ref-label>CLAVE DE RASTREO</span>
            </label>
            <div class="issue__box">
                <input class="issue__input" id="referencia" name="referencia" type="text"
                       maxlength="40" required autocomplete="off"
                       value="${fn:escapeXml(f.referencia)}"
                       placeholder="MBAN01002608180000012345">
            </div>
            <p class="issue__hint" data-ref-hint>
                La que emite el banco y aparece en el CEP. No se puede repetir:
                el sistema rechaza un depósito ya contabilizado.
            </p>
        </div>

        <p class="issue__section">Quién lo mandó</p>

        <div class="issue__field">
            <label class="issue__label" for="ordenanteNombre">RAZÓN SOCIAL</label>
            <div class="issue__box">
                <input class="issue__input" id="ordenanteNombre" name="ordenanteNombre" type="text"
                       maxlength="160" required autocomplete="off"
                       value="${fn:escapeXml(f.ordenanteNombre)}"
                       placeholder="COMERCIALIZADORA ACME SA DE CV">
            </div>
        </div>

        <div class="issue__field">
            <label class="issue__label" for="ordenanteRfc">
                RFC <span data-rfc-req></span>
            </label>
            <div class="issue__box">
                <input class="issue__input" id="ordenanteRfc" name="ordenanteRfc" type="text"
                       maxlength="13" autocomplete="off"
                       value="${fn:escapeXml(f.ordenanteRfc)}" placeholder="CAC010101AB1">
            </div>
            <p class="issue__hint" data-rfc-hint></p>
        </div>

        <div class="issue__field" data-only="SPEI">
            <label class="issue__label" for="ordenanteClabe">CLABE ORDENANTE</label>
            <div class="issue__box">
                <input class="issue__input" id="ordenanteClabe" name="ordenanteClabe" type="text"
                       maxlength="22" autocomplete="off"
                       value="${fn:escapeXml(f.ordenanteClabe)}" placeholder="002180000123456789">
            </div>
            <p class="issue__hint">
                18 dígitos. El último es el dígito de control y se verifica: una
                CLABE mal tecleada no llega al ledger.
            </p>
        </div>

        <div class="issue__field">
            <label class="issue__label" for="institucion">INSTITUCIÓN</label>
            <div class="issue__box">
                <input class="issue__input" id="institucion" name="institucion" type="text"
                       maxlength="60" required autocomplete="off"
                       value="${fn:escapeXml(f.institucion)}" placeholder="BANAMEX">
            </div>
        </div>

        <div class="issue__field" data-only="VENTANILLA">
            <label class="issue__label" for="sucursal">SUCURSAL</label>
            <div class="issue__box">
                <input class="issue__input" id="sucursal" name="sucursal" type="text"
                       maxlength="60" autocomplete="off"
                       value="${fn:escapeXml(f.sucursal)}" placeholder="Cuernavaca Centro">
            </div>
        </div>

        <p class="issue__section">La operación</p>

        <div class="issue__field">
            <label class="issue__label" for="monto">MONTO</label>
            <div class="issue__box">
                <input class="issue__input" id="monto" name="monto" type="number"
                       step="0.01" min="0.01" required
                       value="${fn:escapeXml(f.monto)}" placeholder="250000.00">
            </div>
        </div>

        <div class="issue__field">
            <label class="issue__label" for="concepto">CONCEPTO</label>
            <div class="issue__box">
                <input class="issue__input" id="concepto" name="concepto" type="text"
                       maxlength="40" autocomplete="off"
                       value="${fn:escapeXml(f.concepto)}" placeholder="Fondeo nómina agosto">
            </div>
        </div>

        <div class="issue__field" data-only="SPEI">
            <label class="issue__label" for="referenciaNumerica">REFERENCIA NUMÉRICA</label>
            <div class="issue__box">
                <input class="issue__input" id="referenciaNumerica" name="referenciaNumerica"
                       type="number" min="0" max="9999999"
                       value="${fn:escapeXml(f.referenciaNumerica)}" placeholder="8180001">
            </div>
        </div>

        <div class="issue__field">
            <label class="issue__label" for="fechaOperacion">FECHA DE LA OPERACIÓN</label>
            <div class="issue__box">
                <input class="issue__input" id="fechaOperacion" name="fechaOperacion"
                       type="datetime-local" required
                       value="${fn:escapeXml(f.fechaOperacion)}">
            </div>
            <p class="issue__hint">Cuándo lo procesó el banco, que no es cuándo lo registramos.</p>
        </div>

        <%-- El destino no se elige: es la CLABE de la Concentradora. Va oculto
             y el servicio lo vuelve a comprobar, porque un campo del formulario
             no es una garantía de nada. --%>
        <input type="hidden" name="beneficiarioClabe" value="${concentrador.clabe}">

        <div class="issue__actions">
            <button type="submit" class="btn btn--primary btn--hero">Publicar depósito</button>
            <a class="btn btn--secondary btn--hero" href="${ctx}/admin/concentradora">Ver la Concentradora</a>
        </div>
    </form>

    <aside class="issue-card issue-card--aside">
        <p class="issue__section">Cuenta destino</p>
        <p class="issue__aside-label">CLABE DE LA CONCENTRADORA</p>
        <p class="sim-clabe">${concentrador.clabeFormatted}</p>
        <p class="issue__hint">
            Es la cuenta a la que la empresa transfiere. Un depósito dirigido a
            cualquier otra CLABE se rechaza aunque venga bien firmado.
        </p>

        <p class="issue__section" style="margin-top: 28px;">Saldo actual</p>
        <p class="sim-balance">$<fmt:formatNumber value="${concentrador.balance}" type="number"
                groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/></p>

        <p class="issue__hint" style="margin-top: 24px;">
            Lo que se publica aquí entra por el mismo endpoint firmado que usaría
            el banco. Si la firma no cuadra, la bitácora lo registra como
            <strong>CRIT</strong>.
        </p>
    </aside>
</div>

<script>
    // Enseñar sólo los campos que el canal admite. Duplica en el navegador lo
    // que ya deciden FundingService y los CHECK de la base — aquí es cortesía,
    // no control: quien manda sigue siendo el servidor.
    (function () {
        var form = document.getElementById("sim-form");
        var refLabel = form.querySelector("[data-ref-label]");
        var refHint  = form.querySelector("[data-ref-hint]");
        var refInput = document.getElementById("referencia");
        var rfcReq   = form.querySelector("[data-rfc-req]");
        var rfcHint  = form.querySelector("[data-rfc-hint]");
        var rfcInput = document.getElementById("ordenanteRfc");
        var clabeInput = document.getElementById("ordenanteClabe");
        var sucursalInput = document.getElementById("sucursal");

        function apply() {
            var spei = form.querySelector("input[name=canal]:checked").value === "SPEI";

            form.querySelectorAll("[data-only]").forEach(function (el) {
                el.hidden = el.getAttribute("data-only") !== (spei ? "SPEI" : "VENTANILLA");
            });

            refLabel.textContent = spei ? "CLAVE DE RASTREO" : "FOLIO DE LA FICHA DE DEPÓSITO";
            refInput.placeholder = spei ? "MBAN01002608180000012345" : "FICHA-2026-081801";
            refHint.textContent = spei
                ? "La que emite el banco y aparece en el CEP. No se puede repetir: el sistema rechaza un depósito ya contabilizado."
                : "El folio impreso en la ficha. No se puede repetir: el sistema rechaza un depósito ya contabilizado.";

            // La asimetría del diseño, dicha en la pantalla.
            rfcReq.textContent = spei ? "(opcional)" : "(obligatorio)";
            rfcInput.required = !spei;
            rfcHint.textContent = spei
                ? "En SPEI la cuenta ordenante ya identifica el origen."
                : "En efectivo no hay cuenta que identifique a nadie, así que el RFC es lo que queda.";

            // Lo oculto no se manda: un campo escondido con valor viejo haría
            // que el servicio rechace por una regla de canal que el usuario ya
            // no ve en pantalla.
            clabeInput.disabled = !spei;
            sucursalInput.disabled = spei;
            sucursalInput.required = !spei;
        }

        form.querySelectorAll("input[name=canal]").forEach(function (r) {
            r.addEventListener("change", apply);
        });
        apply();

        // Por comodidad: la fecha arranca en ahora si viene vacía.
        var fecha = document.getElementById("fechaOperacion");
        if (!fecha.value) {
            var now = new Date(Date.now() - new Date().getTimezoneOffset() * 60000);
            fecha.value = now.toISOString().slice(0, 16);
        }
    })();
</script>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
