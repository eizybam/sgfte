<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Detalle de Tarjetahabiente · marco Figma 2074:294.

  Misma estructura que el detalle de cuenta, así que reutiliza sus componentes:
  cabecera con miga de pan, panel de saldo, tabla interna, tarjetas vinculadas y
  resumen lateral. Sólo lectura; cada acción sale a otra pantalla.
--%>
<c:set var="pageTitle" value="${person.fullName}"/>
<c:set var="activeNav" value="people"/>
<c:set var="hidePageHead" value="true"/>
<c:set var="mainClass" value="app-main--flush"/>
<%@ include file="/WEB-INF/jsp/partials/admin-top.jspf" %>

<header class="detail-head">
    <p class="crumbs">
        <a href="${ctx}/admin/empleados">Empleados</a>
        <span class="crumbs__sep">›</span>
        Detalle de tarjetahabiente
    </p>

    <h1 class="detail-head__title">
        ${fn:escapeXml(person.fullName)}
        <c:choose>
            <c:when test="${person.active}">
                <span class="state-badge state-badge--done">ACTIVO</span>
            </c:when>
            <c:otherwise>
                <span class="state-badge state-badge--off">INACTIVO</span>
            </c:otherwise>
        </c:choose>
    </h1>

    <p class="detail-head__meta">
        ${fn:escapeXml(person.employeeCode)}
        <span class="crumbs__sep">·</span>
        ${fn:escapeXml(person.email)}
        <c:if test="${not empty person.department}">
            <span class="crumbs__sep">·</span>
            ${fn:escapeXml(person.department)}
        </c:if>
    </p>
</header>

<div class="detail-grid">

    <div>
        <section class="panel balance">
            <p class="panel__label">SALDO TOTAL ASIGNADO</p>

            <p class="balance__figure">
                <span>$<fmt:formatNumber value="${person.totalBalance}" type="number"
                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/></span>
                <span class="balance__currency">MXN</span>
            </p>

            <p class="balance__purpose">
                <span class="balance__dot" style="background: var(--sgfte-ok);"></span>
                ${person.activeAccounts} ${person.activeAccounts == 1 ? 'cuenta activa' : 'cuentas activas'}
                <c:if test="${not empty purposeSummary}">
                    <span class="crumbs__sep">·</span> ${fn:escapeXml(purposeSummary)}
                </c:if>
            </p>

            <%-- Los dos botones del marco miden 160x48 y van apilados a la derecha. --%>
            <div class="balance__actions balance__actions--narrow">
                <a class="btn btn--primary" href="${ctx}/accounts">Nueva cuenta</a>
                <button type="button" class="btn btn--secondary" data-open-edit>Editar perfil</button>
            </div>
        </section>

        <section class="panel moves" style="margin-top: 28px;">
            <div class="moves__head">
                <p class="panel__label">CUENTAS DEL TARJETAHABIENTE</p>
                <a class="moves__more" href="${ctx}/admin/cuentas?q=${fn:escapeXml(person.fullName)}">Gestionar cuentas</a>
            </div>

            <c:choose>
                <c:when test="${empty accounts}">
                    <p class="moves__empty">Este empleado todavía no tiene cuentas asignadas.</p>
                </c:when>
                <c:otherwise>
                    <table class="moves__table">
                        <thead>
                        <tr>
                            <th class="moves__col-date">PROPÓSITO</th>
                            <th>N° DE CUENTA</th>
                            <th class="moves__col-amount">SALDO</th>
                            <th class="moves__col-state">ESTADO</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="a" items="${accounts}">
                            <tr>
                                <td class="moves__date">${fn:escapeXml(a.purpose)}</td>
                                <td class="moves__concept">
                                    <a class="cell-link" href="${ctx}/admin/cuenta?id=${a.id}">${fn:escapeXml(a.accountNumber)}</a>
                                </td>
                                <td class="moves__amount">
                                    $<fmt:formatNumber value="${a.balance}" type="number"
                                        groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/>
                                </td>
                                <%--
                                  El marco muestra también SUSPENDIDA, pero el
                                  esquema sólo admite ACTIVE e INACTIVE: no hay
                                  estado intermedio que representar.
                                --%>
                                <td class="moves__state">
                                    <c:choose>
                                        <c:when test="${a.active}">
                                            <span class="state-badge state-badge--done">ACTIVA</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="state-badge state-badge--off">INACTIVA</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
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

            <a class="linked__add" href="${ctx}/admin/cards">+&nbsp;&nbsp;Expedir nueva tarjeta</a>
        </section>

        <section class="panel month" style="margin-top: 28px;">
            <p class="panel__label">RESUMEN DEL PERFIL</p>
            <dl class="month__list">
                <div class="month__row">
                    <dt>Saldo total</dt>
                    <dd>$<fmt:formatNumber value="${person.totalBalance}" type="number"
                            groupingUsed="true" minFractionDigits="2" maxFractionDigits="2"/> MXN</dd>
                </div>
                <div class="month__row"><dt>Cuentas activas</dt><dd>${person.activeAccounts}</dd></div>
                <div class="month__row"><dt>Tarjetas</dt><dd>${person.cardCount}</dd></div>
                <div class="month__row">
                    <dt>Departamento</dt>
                    <dd>${empty person.department ? '—' : fn:escapeXml(person.department)}</dd>
                </div>
                <div class="month__row">
                    <dt>Estado</dt>
                    <dd>${person.active ? 'Activo' : 'Inactivo'}</dd>
                </div>
            </dl>
        </section>
    </div>
</div>

<c:set var="editFailed" value="${not empty editErrors}"/>

<div class="modal-scrim" id="edit-modal" ${editFailed ? '' : 'hidden'}>
    <div class="modal modal--form" role="dialog" aria-modal="true" aria-labelledby="edit-title">
        <h2 class="modal__title" id="edit-title">Editar perfil</h2>
        <div class="modal__rule"></div>

        <c:if test="${editFailed}">
            <div class="alert alert--error modal__alert" style="margin: var(--sp-3) 40px 0;">
                <ul><c:forEach var="e" items="${editErrors}"><li>${e}</li></c:forEach></ul>
            </div>
        </c:if>

        <form class="modal__body" method="post" action="${ctx}/cardholders">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="cardholderId" value="${person.id}">

            <label class="register__label" for="editName">Nombre completo</label>
            <div class="register__control">
                <input class="register__input" type="text" id="editName" name="fullName"
                       value="${fn:escapeXml(person.fullName)}" autocomplete="off" required>
            </div>

            <div class="register__row">
                <div>
                    <label class="register__label" for="editCode">Id de empleado</label>
                    <div class="register__control">
                        <%-- No se edita: se asigna al registrar y es lo que la
                             persona reconoce como suyo. --%>
                        <input class="register__input" type="text" id="editCode"
                               value="${fn:escapeXml(person.employeeCode)}"
                               readonly tabindex="-1" title="El código no cambia">
                    </div>
                </div>
                <div>
                    <label class="register__label" for="editDept">Departamento</label>
                    <div class="register__control">
                        <select class="register__input" id="editDept" name="department" required>
                            <option value="IT" ${person.department == 'IT' ? 'selected' : ''}>IT</option>
                        </select>
                        <svg class="register__chevron" width="12.64" height="6.82" aria-hidden="true"><use href="#i-chevron"/></svg>
                    </div>
                </div>
            </div>

            <label class="register__label" for="editEmail">Correo corporativo</label>
            <div class="register__control">
                <input class="register__input" type="email" id="editEmail" name="email"
                       value="${fn:escapeXml(person.email)}" autocomplete="off" required>
            </div>

            <label class="register__label" for="editPhone">Teléfono</label>
            <div class="register__control">
                <input class="register__input" type="tel" id="editPhone" name="phone"
                       value="${fn:escapeXml(person.phone)}" autocomplete="off"
                       placeholder="Opcional · 10 dígitos">
            </div>

            <p class="register__note">
                <svg width="18" height="18" aria-hidden="true"><use href="#i-info"/></svg>
                <span>El correo es también el usuario con el que el empleado entra al
                    sistema: al cambiarlo aquí, cambia su acceso.</span>
            </p>

            <div class="register__actions">
                <button type="button" class="btn btn--secondary" data-close-edit>Cancelar</button>
                <button type="submit" class="btn btn--primary">Guardar cambios</button>
            </div>
        </form>
    </div>
</div>

<script>
    (function () {
        var scrim = document.getElementById("edit-modal");
        var first = document.getElementById("editName");
        var lastFocused = null;

        function open()  { lastFocused = document.activeElement; scrim.hidden = false; first.focus(); }
        function close() { scrim.hidden = true; if (lastFocused) lastFocused.focus(); }

        document.querySelectorAll("[data-open-edit]").forEach(function (b) {
            b.addEventListener("click", open);
        });
        document.querySelectorAll("[data-close-edit]").forEach(function (b) {
            b.addEventListener("click", close);
        });

        scrim.addEventListener("mousedown", function (e) { if (e.target === scrim) close(); });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && !scrim.hidden) close();
        });

        // Si el POST falló, el modal nace abierto: no se pierde lo tecleado.
        if (!scrim.hidden) first.focus();
    })();
</script>

<%@ include file="/WEB-INF/jsp/partials/admin-bottom.jspf" %>
