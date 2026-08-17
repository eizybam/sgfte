<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Categorías · marco Figma "Categorías - Admin" (2036:6) y su modal
  "Nueva Categoria - Popup" (2041:155).

  El catálogo de propósitos: lo que decide de qué es cada cuenta, de qué color
  se pinta en toda la app y entre qué cuentas se puede transferir (la P2P exige
  el mismo category_id).

  No hay borrar, y no es un olvido: account.category_id es NOT NULL y apunta
  aquí, así que borrar una categoría en uso significaría borrar sus cuentas. El
  badge de ESTADO es el interruptor —retirar y reactivar—, que es justo lo que
  el marco dibuja y lo que la tabla ya soportaba con su columna status.
--%>
<c:set var="pageTitle" value="Categorías"/>
<c:set var="pageSubtitle" value="Catálogo global de propósitos de cuenta"/>
<c:set var="activeNav" value="categories"/>
<c:set var="pageAction">
    <button type="button" class="btn btn--primary btn--hero" data-open-create>
        <img src="${pageContext.request.contextPath}/assets/img/icons/plus.png" alt="">
        Nueva categoría
    </button>
</c:set>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<div class="table-card">
    <table class="table table--categories">
        <thead>
        <tr>
            <th class="col-cat">Categoría</th>
            <th class="col-desc">Descripción</th>
            <th class="col-accounts">Cuentas</th>
            <th class="col-dispersed">Dispersión total</th>
            <th class="col-state">Estado</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="cat" items="${rows}">
            <tr>
                <td>
                    <div class="cat-cell">
                        <span class="cat-swatch"
                              style="background: var(--sgfte-purpose-${cat.colorIndex});"></span>
                        <span>
                            <span class="cat-name">${fn:escapeXml(cat.name)}</span>
                            <span class="cat-kind">Propósito de fondos</span>
                        </span>
                    </div>
                </td>

                <td class="cat-desc ${empty cat.description ? 'cat-desc--empty' : ''}">
                    ${empty cat.description ? 'Sin descripción' : fn:escapeXml(cat.description)}
                </td>

                <td class="num">${cat.accounts}</td>

                <td class="num">$<fmt:formatNumber value="${cat.dispersed}" type="number"
                        groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/></td>

                <%--
                  El badge es un botón dentro de su propio formulario: un POST
                  por fila, sin JavaScript. Va el nombre además del id para que
                  la bitácora diga "Gasolina" y no "3".
                --%>
                <td>
                    <form method="post" action="${ctx}/admin/categorias">
                        <input type="hidden" name="action" value="toggle">
                        <input type="hidden" name="categoryId" value="${cat.id}">
                        <input type="hidden" name="categoryName" value="${fn:escapeXml(cat.name)}">
                        <button type="submit" class="cat-toggle"
                                title="${cat.active ? 'Retirar del catálogo' : 'Reactivar'}">
                            <span class="badge ${cat.active ? 'badge--ok' : 'badge--neutral'}">
                                ${cat.active ? 'ACTIVA' : 'INACTIVA'}
                            </span>
                        </button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}">
            <tr><td colspan="5" class="table__empty">El catálogo está vacío; crea la primera categoría.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<p class="cat-count">
    Mostrando ${fn:length(rows)} de ${fn:length(rows)}
    ${fn:length(rows) == 1 ? 'categoría' : 'categorías'}
</p>

<%--
  ---- Departamentos -------------------------------------------------------

  El segundo catálogo de la empresa, en la misma pantalla que el primero: los
  dos son listas que alimentan un desplegable y ninguno da para una pantalla
  propia. Misma tabla, mismo modal, mismo interruptor en el badge.

  Sin color: el de una categoría distingue propósitos en tablas y gráficas; un
  departamento no se pinta en ninguna parte. Y en su sitio, "Empleados" y
  "Fondo total", que son lo que se mira antes de retirar un área.

  Las escrituras van a /admin/departamentos —otra entidad, otro servlet— y
  vuelven aquí.
--%>
<div class="cat-section">
    <div class="cat-section__head">
        <div>
            <h2 class="cat-section__title">Departamentos</h2>
            <p class="cat-section__lead">Catálogo de áreas de la empresa</p>
        </div>
        <button type="button" class="btn btn--primary btn--hero" data-open-dept>
            <img src="${ctx}/assets/img/icons/plus.png" alt="">
            Nuevo departamento
        </button>
    </div>

    <div class="table-card">
        <table class="table table--categories table--departments">
            <thead>
            <tr>
                <th class="col-cat">Departamento</th>
                <th class="col-desc">Descripción</th>
                <th class="col-accounts">Empleados</th>
                <th class="col-dispersed">Fondo total</th>
                <th class="col-state">Estado</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="dep" items="${departments}">
                <tr>
                    <%-- El nombre ES el botón de editar: no hace falta una
                         columna de acciones para una sola acción, igual que el
                         badge de estado no la necesitó para el interruptor. --%>
                    <td>
                        <button type="button" class="cat-edit" data-edit-dept
                                data-id="${dep.id}"
                                data-name="${fn:escapeXml(dep.name)}"
                                data-description="${fn:escapeXml(dep.description)}"
                                title="Editar ${fn:escapeXml(dep.name)}">
                            <span class="cat-name">${fn:escapeXml(dep.name)}</span>
                            <span class="cat-kind">Área de la empresa</span>
                        </button>
                    </td>

                    <td class="cat-desc ${empty dep.description ? 'cat-desc--empty' : ''}">
                        ${empty dep.description ? 'Sin descripción' : fn:escapeXml(dep.description)}
                    </td>

                    <td class="num">${dep.employees}</td>

                    <td class="num">$<fmt:formatNumber value="${dep.funds}" type="number"
                            groupingUsed="true" minFractionDigits="0" maxFractionDigits="2"/></td>

                    <%-- El badge es el interruptor, igual que en categorías: un
                         POST por fila, con el nombre además del id para que la
                         bitácora diga "Ventas" y no "3". --%>
                    <td>
                        <form method="post" action="${ctx}/admin/departamentos">
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="departmentId" value="${dep.id}">
                            <input type="hidden" name="departmentName" value="${fn:escapeXml(dep.name)}">
                            <button type="submit" class="cat-toggle"
                                    title="${dep.active ? 'Retirar del catálogo' : 'Reactivar'}"
                                    data-confirm ${dep.active ? 'data-confirm-danger' : ''}
                                    data-confirm-title="${dep.active ? '¿Retirar' : '¿Reactivar'} ${fn:escapeXml(dep.name)}?"
                                    data-confirm-text="${dep.active
                                        ? 'Deja de ofrecerse al registrar o editar empleados.'
                                        : 'Vuelve a ofrecerse al registrar o editar empleados.'}"
                                    data-confirm-list="${dep.active
                                        ? 'Los empleados que ya pertenecen al área la conservan.|No se borra nada: sólo desaparece de los desplegables.|Puedes reactivarlo cuando quieras.'
                                        : ''}"
                                    data-confirm-ok="${dep.active ? 'Sí, retirar' : 'Sí, reactivar'}">
                                <span class="badge ${dep.active ? 'badge--ok' : 'badge--neutral'}">
                                    ${dep.active ? 'ACTIVO' : 'INACTIVO'}
                                </span>
                            </button>
                        </form>
                    </td>

                </tr>
            </c:forEach>
            <c:if test="${empty departments}">
                <tr><td colspan="5" class="table__empty">
                    El catálogo de áreas está vacío. Crea el primero para poder asignarlo a un empleado.
                </td></tr>
            </c:if>
            </tbody>
        </table>
    </div>

    <p class="cat-count">
        Mostrando ${fn:length(departments)} de ${fn:length(departments)}
        ${fn:length(departments) == 1 ? 'departamento' : 'departamentos'}
    </p>
</div>


<%-- Modal "Nueva categoría" (2041:155), mismo componente que Crear cuenta. --%>
<c:set var="createFailed" value="${not empty createErrors}"/>
<c:set var="selectedColor" value="${empty createColor ? 1 : createColor}"/>

<div class="modal-scrim" id="create-modal" ${createFailed ? '' : 'hidden'}>
    <div class="modal modal--stack" role="dialog" aria-modal="true" aria-labelledby="create-title">
        <h2 class="modal__title" id="create-title">Nueva categoría</h2>
        <p class="modal__lead">Define un nuevo propósito de cuenta</p>

        <c:if test="${createFailed}">
            <div class="alert alert--error modal__alert" style="margin: var(--sp-3) 40px 0;">
                <ul><c:forEach var="e" items="${createErrors}"><li>${e}</li></c:forEach></ul>
            </div>
        </c:if>

        <form class="modal__body" method="post" action="${ctx}/admin/categorias">
            <input type="hidden" name="action" value="create">

            <label class="modal__label modal__label--tracked" for="name">NOMBRE DE LA CATEGORÍA</label>
            <div class="register__control">
                <input class="register__input" type="text" id="name" name="name"
                       maxlength="40" placeholder="Mantenimiento" required
                       value="${fn:escapeXml(createName)}">
            </div>

            <label class="modal__label modal__label--tracked" for="description">DESCRIPCIÓN</label>
            <div class="register__control">
                <input class="register__input" type="text" id="description" name="description"
                       maxlength="120" placeholder="Refacciones y servicio vehicular"
                       value="${fn:escapeXml(createDescription)}">
            </div>

            <%--
              Paleta: radios de verdad, no divs con onclick. Así el grupo se
              recorre con las flechas, el color viaja en el POST sin JavaScript
              y el navegador ya garantiza que sólo haya uno elegido.
            --%>
            <span class="modal__label modal__label--tracked" id="color-label">COLOR DE LA CATEGORÍA</span>
            <div class="swatches" role="radiogroup" aria-labelledby="color-label">
                <c:forEach var="i" items="${colors}">
                    <label class="swatch">
                        <input class="swatch__input" type="radio" name="colorIndex" value="${i}"
                               ${selectedColor == i ? 'checked' : ''}>
                        <span class="swatch__box" style="background: var(--sgfte-purpose-${i});"
                              title="Color ${i}"></span>
                    </label>
                </c:forEach>
            </div>

            <span class="modal__label modal__label--tracked">ESTADO</span>
            <div class="switch">
                <input class="switch__input" type="checkbox" id="active" name="active" checked>
                <label class="switch__track" for="active" aria-label="Categoría activa"></label>
                <span class="switch__label">Activa</span>
            </div>

            <div class="register__actions">
                <button type="button" class="btn btn--secondary" data-close-create>Cancelar</button>
                <button type="submit" class="btn btn--primary">
                    <img src="${ctx}/assets/img/icons/plus.png" alt="">
                    Crear categoría
                </button>
            </div>
        </form>
    </div>
</div>

<script>
    (function () {
        var scrim = document.getElementById("create-modal");
        var firstField = document.getElementById("name");
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

        document.querySelectorAll("[data-open-create]").forEach(function (b) {
            b.addEventListener("click", open);
        });
        document.querySelectorAll("[data-close-create]").forEach(function (b) {
            b.addEventListener("click", close);
        });

        scrim.addEventListener("mousedown", function (e) { if (e.target === scrim) close(); });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && !scrim.hidden) close();
        });

        if (!scrim.hidden) firstField.focus();
    })();
</script>


<%--
  Modal de departamento · sirve para crear y para editar.

  Uno solo y no dos: lo único que cambia entre "Nuevo departamento" y "Editar
  departamento" son el título, el valor de action y el botón. Dos modales serían
  dos sitios donde arreglar el mismo detalle mañana.
--%>
<c:set var="deptFailed" value="${not empty deptErrors}"/>

<div class="modal-scrim" id="dept-modal" ${deptFailed ? '' : 'hidden'}>
    <div class="modal modal--stack" role="dialog" aria-modal="true" aria-labelledby="dept-title">
        <h2 class="modal__title" id="dept-title">Nuevo departamento</h2>
        <p class="modal__lead">Define un área de la empresa</p>

        <c:if test="${deptFailed}">
            <div class="alert alert--error modal__alert" style="margin: var(--sp-3) 40px 0;">
                <ul><c:forEach var="e" items="${deptErrors}"><li>${e}</li></c:forEach></ul>
            </div>
        </c:if>

        <form class="modal__body" method="post" action="${ctx}/admin/departamentos">
            <input type="hidden" name="action" id="deptAction" value="create">
            <input type="hidden" name="departmentId" id="deptId" value="">

            <label class="modal__label modal__label--tracked" for="deptNameInput">NOMBRE DEL DEPARTAMENTO</label>
            <div class="register__control">
                <input class="register__input" type="text" id="deptNameInput" name="name"
                       maxlength="60" placeholder="Recursos Humanos" required
                       value="${fn:escapeXml(deptName)}">
            </div>

            <label class="modal__label modal__label--tracked" for="deptDescription">DESCRIPCIÓN</label>
            <div class="register__control">
                <input class="register__input" type="text" id="deptDescription" name="description"
                       maxlength="120" placeholder="Personal y prestaciones"
                       value="${fn:escapeXml(deptDescription)}">
            </div>

            <%-- Sólo al crear: el estado de un área que ya existe se cambia con
                 el badge de la tabla, que además lo registra en la bitácora. --%>
            <div id="deptStateField">
                <span class="modal__label modal__label--tracked">ESTADO</span>
                <div class="switch">
                    <input class="switch__input" type="checkbox" id="deptActive" name="active" checked>
                    <label class="switch__track" for="deptActive" aria-label="Departamento activo"></label>
                    <span class="switch__label">Activo</span>
                </div>
            </div>

            <div class="register__actions">
                <button type="button" class="btn btn--secondary" data-close-dept>Cancelar</button>
                <button type="submit" class="btn btn--primary" id="deptSubmit">Crear departamento</button>
            </div>
        </form>
    </div>
</div>

<script>
    (function () {
        var scrim  = document.getElementById("dept-modal");
        if (!scrim) return;
        var title  = document.getElementById("dept-title");
        var action = document.getElementById("deptAction");
        var idIn   = document.getElementById("deptId");
        var name   = document.getElementById("deptNameInput");
        var desc   = document.getElementById("deptDescription");
        var state  = document.getElementById("deptStateField");
        var submit = document.getElementById("deptSubmit");
        var lastFocused = null;

        /* El mismo modal en dos modos: con fila = editar, sin fila = crear. */
        function fillFor(row) {
            var editing = !!row;

            title.textContent  = editing ? "Editar departamento" : "Nuevo departamento";
            action.value       = editing ? "update" : "create";
            idIn.value         = editing ? row.getAttribute("data-id") : "";
            name.value         = editing ? row.getAttribute("data-name") : "";
            desc.value         = editing ? row.getAttribute("data-description") : "";
            submit.textContent = editing ? "Guardar cambios" : "Crear departamento";
            state.hidden       = editing;

            lastFocused = document.activeElement;
            scrim.hidden = false;
            name.focus();
        }

        function close() {
            scrim.hidden = true;
            if (lastFocused) lastFocused.focus();
        }

        document.querySelectorAll("[data-open-dept]").forEach(function (b) {
            b.addEventListener("click", function () { fillFor(null); });
        });
        document.querySelectorAll("[data-edit-dept]").forEach(function (b) {
            b.addEventListener("click", function () { fillFor(b); });
        });
        document.querySelectorAll("[data-close-dept]").forEach(function (b) {
            b.addEventListener("click", close);
        });

        scrim.addEventListener("mousedown", function (e) { if (e.target === scrim) close(); });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && !scrim.hidden) close();
        });

        /*
          Si el POST falló, el modal vuelve abierto con lo tecleado. Y si el que
          falló era una edición, vuelve en modo edición: deptEditId dice cuál.
        */
        if (!scrim.hidden) {
            var failedId = "${deptEditId}";
            if (failedId) {
                var row = document.querySelector('[data-edit-dept][data-id="' + failedId + '"]');
                if (row) {
                    title.textContent  = "Editar departamento";
                    action.value       = "update";
                    idIn.value         = failedId;
                    submit.textContent = "Guardar cambios";
                    state.hidden       = true;
                }
            }
            name.focus();
        }
    })();
</script>

<%@ include file="/WEB-INF/jsp/partials/confirm-modal.jspf" %>
<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
