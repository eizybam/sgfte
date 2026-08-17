<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Gestión de Cuentas · marco Figma "Gestion de Cuentas" (225:41).

  Los tres filtros y la página viven en la URL, no en JavaScript: así una vista
  filtrada se puede enlazar y recargar, y el paginador son enlaces normales.

  El buscador es un formulario GET que arrastra el estado del segmentado y de la
  píldora en campos ocultos, para que buscar no borre los otros dos filtros.
--%>
<c:set var="pageTitle" value="Gestión de Cuentas"/>
<c:set var="pageSubtitle" value="Administra las cuentas y su dispersion de fondos"/>
<c:set var="activeNav" value="accounts"/>
<c:set var="pageAction">
    <button type="button" class="btn btn--primary btn--hero btn--stacked" data-open-create>
        <img src="${pageContext.request.contextPath}/assets/img/icons/plus.png" alt="">
        Crear<br>cuenta
    </button>
</c:set>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<%-- Base para rehacer la URL conservando los filtros que no se están tocando. --%>
<c:set var="baseUrl" value="${ctx}/admin/cuentas"/>
<c:set var="qParam"       value="${empty q ? '' : '&q='.concat(q)}"/>
<c:set var="statusParam"  value="${empty status ? '' : '&status='.concat(status)}"/>
<c:set var="purposeParam" value="${empty purpose ? '' : '&purpose='.concat(purpose)}"/>

<div class="toolbar">
    <form class="search" method="get" action="${baseUrl}">
        <svg class="search__icon" width="18" height="18" aria-hidden="true"><use href="#i-search"/></svg>
        <input class="input" type="search" name="q" value="${fn:escapeXml(q)}"
               placeholder="Buscar por titular, ID de cuenta o empleado" aria-label="Buscar cuentas">
        <c:if test="${not empty status}"><input type="hidden" name="status" value="${status}"></c:if>
        <c:if test="${not empty purpose}"><input type="hidden" name="purpose" value="${purpose}"></c:if>
    </form>

    <nav class="segmented" aria-label="Estado">
        <a class="segmented__item ${empty status ? 'is-active' : ''}"
           href="${baseUrl}?page=1${qParam}${purposeParam}">Todas</a>
        <a class="segmented__item ${status == 'ACTIVE' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&status=ACTIVE${qParam}${purposeParam}">Abiertas</a>
        <%-- El valor del filtro sigue siendo INACTIVE: esto es vocabulario de
             pantalla, no de esquema. La base no cambia. --%>
        <a class="segmented__item ${status == 'INACTIVE' ? 'is-active' : ''}"
           href="${baseUrl}?page=1&status=INACTIVE${qParam}${purposeParam}">Cerradas</a>
    </nav>

    <%-- La píldora del marco es un desplegable; aquí es un <select> que navega. --%>
    <label class="pill">
        <span>Propósito ·</span>
        <select class="pill__select" onchange="location.href=this.value;" aria-label="Filtrar por propósito">
            <option value="${baseUrl}?page=1${qParam}${statusParam}" ${empty purpose ? 'selected' : ''}>TODOS</option>
            <c:forEach var="cat" items="${categories}">
                <option value="${baseUrl}?page=1&purpose=${cat.id}${qParam}${statusParam}"
                        ${purpose == cat.id ? 'selected' : ''}>${fn:escapeXml(cat.name)}</option>
            </c:forEach>
        </select>
        <svg class="pill__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
    </label>

    <span class="toolbar__count">
        <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/>
        ${total == 1 ? 'resultado' : 'resultados'}
    </span>
</div>

<div class="table-card">
    <table class="table table--accounts">
        <thead>
        <tr>
            <th class="col-code">Cuenta</th>
            <th class="col-holder">Titular</th>
            <th class="col-purpose">Proposito</th>
            <th class="col-cards">Tarjetas</th>
            <th class="col-status">Estado</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${rows}">
            <tr>
                <%-- En el marco el código de cuenta es el enlace al detalle. --%>
                <td class="mono">
                    <a class="cell-link" href="${ctx}/admin/cuenta?id=${a.id}">${fn:escapeXml(a.accountNumber)}</a>
                </td>
                <td>${fn:escapeXml(a.holderName)}</td>
                <td><span class="badge badge--p${a.purposeColor}">${fn:escapeXml(a.purpose)}</span></td>
                <td class="num">${a.activeCards}</td>
                <%--
                  El badge de estado ES la acción, igual que en /admin/empleados
                  —una acción por fila no merece una columna entera—, pero con
                  una diferencia deliberada: allí el badge es un interruptor de
                  dos posiciones y aquí no. Cerrar una cuenta es definitivo, así
                  que sólo la abierta es un <button>; la cerrada es un <span>
                  que ni se ilumina ni recibe el foco. Un badge que responde al
                  ratón y no hace nada invita a un clic que nunca contesta.
                --%>
                <td>
                    <c:choose>
                        <c:when test="${a.active}">
                            <%-- Los datos van en hidden, no en el botón:
                                 confirm-modal.jspf envía con form.submit() y el
                                 name/value del botón pulsado no viaja. --%>
                            <form method="post" action="${ctx}/admin/cuentas">
                                <input type="hidden" name="accountId" value="${a.id}">
                                <button type="submit" class="cat-toggle"
                                        title="Cerrar la cuenta y reintegrar su saldo"
                                        data-confirm data-confirm-danger
                                        data-confirm-title="¿Cerrar la cuenta ${fn:escapeXml(a.accountNumber)}?"
                                        data-confirm-text="Al confirmar:"
                                        data-confirm-list="El saldo vuelve completo a la Concentradora.|Sus tarjetas quedan invalidadas.|Se cierra para siempre: una cuenta cerrada no se reabre.|Su historial de movimientos se conserva."
                                        data-confirm-ok="Cerrar y reintegrar">
                                    <span class="badge badge--ok">ABIERTA</span>
                                </button>
                            </form>
                        </c:when>
                        <%-- Neutral y no --error: cerrar una cuenta es una
                             operación normal y bien terminada, no un fallo. --%>
                        <c:otherwise>
                            <span class="badge badge--neutral"
                                  title="Su saldo se reintegró a la Concentradora. Una cuenta cerrada no se reabre; si vuelve a hacer falta ese propósito, se crea una cuenta nueva.">
                                CERRADA
                            </span>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}">
            <tr><td colspan="5" class="table__empty">No hay cuentas que coincidan con el filtro.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<%-- Paginador: ventana de 3 páginas alrededor de la actual, con salto si faltan. --%>
<c:if test="${pageCount > 1}">
    <nav class="pager" aria-label="Paginación">
        <a class="pager__item ${page == 1 ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page - 1}${qParam}${statusParam}${purposeParam}" aria-label="Anterior">
            <svg aria-hidden="true"><use href="#i-prev"/></svg>
        </a>

        <c:set var="from" value="${page > 2 ? page - 1 : 1}"/>
        <c:set var="to" value="${from + 2 > pageCount ? pageCount : from + 2}"/>

        <c:forEach var="p" begin="${from}" end="${to}">
            <a class="pager__item ${p == page ? 'is-current' : ''}"
               href="${baseUrl}?page=${p}${qParam}${statusParam}${purposeParam}">${p}</a>
        </c:forEach>

        <c:if test="${to < pageCount}"><span class="pager__item pager__gap">…</span></c:if>

        <a class="pager__item ${page == pageCount ? 'is-disabled' : ''}"
           href="${baseUrl}?page=${page + 1}${qParam}${statusParam}${purposeParam}" aria-label="Siguiente">
            <svg aria-hidden="true"><use href="#i-next"/></svg>
        </a>
    </nav>
</c:if>


<%--
  Modal "Crear cuenta" (Figma 288:28). Antes era la página /accounts; ahora esa
  ruta sólo procesa el alta y vuelve aquí.

  El identificador no se teclea: lo genera AccountService con el prefijo del
  propósito y un sufijo aleatorio. Como el sufijo se decide al guardar, el campo
  va en sólo lectura y muestra el prefijo en vivo; el código definitivo aparece
  en la tabla al recargar.
--%>
<c:set var="createFailed" value="${not empty createErrors}"/>

<div class="modal-scrim" id="create-modal" ${createFailed ? '' : 'hidden'}>
    <div class="modal modal--form" role="dialog" aria-modal="true" aria-labelledby="create-title">
        <h2 class="modal__title" id="create-title">Crear cuenta</h2>
        <div class="modal__rule"></div>

        <c:if test="${createFailed}">
            <div class="alert alert--error modal__alert" style="margin: var(--sp-3) 40px 0;">
                <ul><c:forEach var="e" items="${createErrors}"><li>${e}</li></c:forEach></ul>
            </div>
        </c:if>

        <form class="modal__body" method="post" action="${ctx}/accounts">

            <label class="register__label" for="cardholderId">Tarjetahabiente</label>
            <div class="register__control">
                <svg class="register__search" width="18" height="18" aria-hidden="true"><use href="#i-search"/></svg>
                <%--
                  El marco dibuja un buscador libre, pero el valor tiene que
                  resolverse a UN empleado concreto, así que es un selector.
                --%>
                <select class="register__input" id="cardholderId" name="cardholderId" required>
                    <option value="" disabled ${empty createHolder ? 'selected' : ''}>Selecciona al empleado</option>
                    <c:forEach var="h" items="${cardholders}">
                        <option value="${h.id}" ${createHolder == h.id ? 'selected' : ''}>${fn:escapeXml(h.firstName)} ${fn:escapeXml(h.lastName)}</option>
                    </c:forEach>
                </select>
                <svg class="register__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
            </div>

            <label class="register__label" for="categoryId">Propósito</label>
            <div class="register__control">
                <select class="register__input" id="categoryId" name="categoryId" required>
                    <option value="" disabled ${empty createCategory ? 'selected' : ''}>Seleccionar propósito</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat.id}" data-name="${fn:escapeXml(cat.name)}"
                                ${createCategory == cat.id ? 'selected' : ''}>${fn:escapeXml(cat.name)}</option>
                    </c:forEach>
                </select>
                <svg class="register__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
            </div>

            <label class="register__label" for="accountNumber">Identificador de cuenta</label>
            <div class="register__control">
                <input class="register__input" type="text" id="accountNumber" readonly tabindex="-1"
                       placeholder="Se genera al crear"
                       title="Se genera solo: prefijo del propósito + sufijo aleatorio">
            </div>

            <p class="register__note">
                <svg width="18" height="18" aria-hidden="true"><use href="#i-info"/></svg>
                <span>La cuenta se crea sin tarjetas y con saldo inicial $0.00; los fondos
                    se asignan después por dispersión desde la Concentradora.</span>
            </p>

            <div class="register__actions">
                <button type="button" class="btn btn--secondary" data-close-create>Cancelar</button>
                <button type="submit" class="btn btn--primary">Crear cuenta</button>
            </div>
        </form>
    </div>
</div>

<script>
    (function () {
        var scrim = document.getElementById("create-modal");
        var purpose = document.getElementById("categoryId");
        var number = document.getElementById("accountNumber");
        var holder = document.getElementById("cardholderId");
        var lastFocused = null;

        // Mismo criterio que AccountService.prefixFrom: tres primeras letras del
        // propósito, sin acentos y en mayúsculas.
        function prefixOf(name) {
            var letters = name.normalize("NFD").replace(/[\u0300-\u036f]/g, "")
                              .replace(/[^A-Za-z]/g, "").toUpperCase();
            if (letters.length < 3) letters += "XXX";
            return letters.slice(0, 3);
        }

        function preview() {
            var opt = purpose.selectedOptions[0];
            var name = opt && opt.dataset ? opt.dataset.name : null;
            // El sufijo lo decide el servidor al guardar, así que aquí van puntos.
            number.value = name ? prefixOf(name) + "-•••••" : "";
        }

        function open() {
            lastFocused = document.activeElement;
            scrim.hidden = false;
            holder.focus();
        }

        function close() {
            scrim.hidden = true;
            if (lastFocused) lastFocused.focus();
        }

        document.querySelectorAll("[data-open-create]").forEach(function (b) {
            b.addEventListener("click", open);
        });
        document.querySelectorAll("[data-close-create]").forEach(function (b) {
            b.addEventListener("click", close);
        });

        purpose.addEventListener("change", preview);
        scrim.addEventListener("mousedown", function (e) { if (e.target === scrim) close(); });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && !scrim.hidden) close();
        });

        preview();
        if (!scrim.hidden) holder.focus();
    })();
</script>

<%@ include file="/WEB-INF/jsp/partials/confirm-modal.jspf" %>
<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
