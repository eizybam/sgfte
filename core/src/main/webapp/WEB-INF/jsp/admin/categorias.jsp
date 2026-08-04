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

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
