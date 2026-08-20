<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Transferencia P2P"/>
<c:set var="pageSubtitle" value="Solo entre cuentas del mismo propósito"/>
<c:set var="activeNav" value="accounts"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<%--
  Vista de administrador. El tarjetahabiente tiene la suya en /app/transferencia,
  donde el origen se limita a sus propias cuentas.
--%>
<div class="card">
    <form method="post" action="${pageContext.request.contextPath}/admin/transferencia">
        <%--
          Origen y destino se ELIGEN, no se despliegan.

          Antes eran dos <select> con TODAS las cuentas activas de la empresa,
          las dos veces: una lista con scroll donde "Gómez, Carlos — Gasolina"
          podía salir dos veces y no había forma de buscar ni de ver el saldo.
          Ahora los dos abren el mismo selector con tabla que usan Dispersión y
          Expedir Tarjeta — cuenta, titular, propósito, tarjetas y saldo, con
          buscador y paginado.

          Un solo modal para los dos campos: cada disparador dice a qué campo
          escribe con data-picker-target, y el guion de abajo separa por eso.
        --%>
        <div class="field">
            <label class="label" for="sourceTrigger">Cuenta origen</label>
            <button type="button" class="input picker__trigger" id="sourceTrigger"
                    data-picker="account"
                    data-picker-target="xfer-source"
                    data-picker-title="Elegir cuenta origen"
                    data-picker-placeholder="Buscar por cuenta, titular, correo o ID de empleado">
                <span id="sourceLabel" class="picker__placeholder">Selecciona la cuenta origen</span>
            </button>
            <input type="hidden" id="sourceId" name="sourceId" required>
        </div>

        <div class="field">
            <label class="label" for="destTrigger">Cuenta destino</label>
            <button type="button" class="input picker__trigger" id="destTrigger"
                    data-picker="account"
                    data-picker-target="xfer-dest"
                    data-picker-title="Elegir cuenta destino"
                    data-picker-placeholder="Buscar por cuenta, titular, correo o ID de empleado">
                <span id="destLabel" class="picker__placeholder">Selecciona la cuenta destino</span>
            </button>
            <input type="hidden" id="destId" name="destId" required>
        </div>

        <div class="field">
            <label class="label" for="amount">Monto (MXN)</label>
            <input class="input" type="number" step="0.01" min="0.01"
                   id="amount" name="amount" placeholder="200.00" required>
        </div>

        <div class="field">
            <label class="label" for="description">Concepto (opcional)</label>
            <input class="input" type="text" id="description" name="description" maxlength="200">
        </div>

        <div class="btn-pair">
            <a class="btn btn--secondary" href="${pageContext.request.contextPath}/admin/home">Cancelar</a>
            <button type="submit" class="btn btn--primary">Transferir</button>
        </div>
    </form>
</div>

<script>
    (function () {
        /* Qué disparador escribe en qué campo. El selector sólo emite el evento. */
        var fields = {
            "xfer-source": { id: "sourceId", label: "sourceLabel", trigger: "sourceTrigger" },
            "xfer-dest":   { id: "destId",   label: "destLabel",   trigger: "destTrigger" }
        };

        document.addEventListener("picker:choose", function (e) {
            var field = fields[e.detail.target];
            if (!field) return;                       // no es para esta pantalla

            var d = e.detail.data;                    // los data-* del <tr>
            document.getElementById(field.id).value = d.id;
            var label = document.getElementById(field.label);
            label.textContent = d.holder + " — " + d.purpose + " · " + d.number;
            label.classList.remove("picker__placeholder");
        });

        /*
          El navegador no valida un input[type=hidden] aunque lleve required, así
          que el "elige las dos cuentas" se hace aquí. Es una cortesía: quien
          decide de verdad sigue siendo TransferService, que rechaza una cuenta
          nula y dos propósitos distintos.
        */
        document.querySelector(".card form").addEventListener("submit", function (e) {
            var missing = ["xfer-source", "xfer-dest"].find(function (key) {
                return !document.getElementById(fields[key].id).value;
            });
            if (missing) {
                e.preventDefault();
                document.getElementById(fields[missing].trigger).focus();
            }
        });
    })();
</script>
<%-- Fuera del <form>: lleva un <input> de búsqueda dentro. --%>
<%@ include file="/WEB-INF/jsp/partials/picker-modal.jspf" %>
<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
