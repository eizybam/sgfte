<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Transferir"/>
<%@ include file="/WEB-INF/jsp/partials/app-top.jspf" %>

<h1 class="page-title">Transferir a un compañero</h1>
<p class="page-subtitle">Solo entre cuentas del mismo propósito.</p>

<c:choose>
    <c:when test="${empty myAccounts}">
        <div class="card mt-4">
            <p class="empty">No tienes cuentas desde las cuales transferir.</p>
        </div>
    </c:when>
    <c:otherwise>

        <%-- Paso 1: elegir origen. Al cambiarlo el formulario se reenvía con GET
             para recalcular los destinos válidos de ese propósito. --%>
        <div class="card mt-4" style="margin-bottom: var(--sp-3);">
            <h2 class="card__title">1 · Desde qué cuenta</h2>
            <form method="get" action="${pageContext.request.contextPath}/app/transferencia">
                <div class="field" style="margin-bottom:0;">
                    <select class="input" name="sourceId" onchange="this.form.submit()">
                        <option value="">— elige tu cuenta —</option>
                        <c:forEach var="a" items="${myAccounts}">
                            <option value="${a.id}" ${a.id == selectedSourceId ? 'selected' : ''}>
                                    ${a.purpose} · ${a.accountNumber} · $ ${a.balance}
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <noscript>
                    <button type="submit" class="btn btn--secondary" style="margin-top: var(--sp-2);">Continuar</button>
                </noscript>
            </form>
        </div>

        <%-- Paso 2: solo cuando ya hay origen elegido. --%>
        <c:if test="${not empty selectedSourceId}">
            <div class="card">
                <h2 class="card__title">2 · Para quién y cuánto</h2>

                <c:choose>
                    <c:when test="${empty peers}">
                        <p class="empty">
                            Ningún compañero tiene una cuenta con este propósito, así que no hay
                            destinos disponibles. Elige otra cuenta de origen.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${pageContext.request.contextPath}/app/transferencia">
                            <input type="hidden" name="sourceId" value="${selectedSourceId}">

                            <div class="field">
                                <label class="label" for="destId">Compañero</label>
                                <select class="input" id="destId" name="destId" required>
                                    <option value="">— elige —</option>
                                    <c:forEach var="p" items="${peers}">
                                        <option value="${p.accountId}">${p.label}</option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="field">
                                <label class="label" for="amount">Monto (MXN)</label>
                                <input class="input" id="amount" name="amount" type="number"
                                       step="0.01" min="0.01" placeholder="0.00" required>
                            </div>

                            <div class="field">
                                <label class="label" for="description">Concepto (opcional)</label>
                                <input class="input" id="description" name="description" type="text"
                                       maxlength="200" placeholder="Ej. gasolina del viaje a Puebla">
                            </div>

                            <div class="btn-pair">
                                <a class="btn btn--secondary" href="${pageContext.request.contextPath}/app/home">Cancelar</a>
                                <button type="submit" class="btn btn--primary">Transferir</button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/partials/app-bottom.jspf" %>
