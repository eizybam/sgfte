<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Transferir · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal.css">
</head>
<body class="auth portal">
<div class="portal-wrap">

    <jsp:include page="nav.jsp"/>

    <h1 class="portal-title">Transferir a un compañero</h1>
    <p class="portal-sub">Solo puedes transferir entre cuentas del <strong>mismo propósito</strong>.</p>

    <c:if test="${not empty success}">
        <div class="portal-alert portal-alert--ok">${success}</div>
    </c:if>
    <c:if test="${not empty errors}">
        <div class="portal-alert portal-alert--error">
            <ul>
                <c:forEach var="e" items="${errors}"><li>${e}</li></c:forEach>
            </ul>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${empty myAccounts}">
            <div class="portal-card">
                <p class="portal-empty">No tienes cuentas desde las cuales transferir.</p>
            </div>
        </c:when>
        <c:otherwise>

            <%-- Step 1: choose the source. Changing it re-submits with GET so the
                 destination list can be rebuilt for that account's purpose. --%>
            <div class="portal-card">
                <h2 class="portal-card__title">1 · Desde qué cuenta</h2>
                <form method="get" action="${pageContext.request.contextPath}/app/transferencia">
                    <div class="auth-field">
                        <div class="auth-input-wrap">
                            <select class="auth-input" style="padding-left: var(--sp-2);"
                                    name="sourceId" onchange="this.form.submit()">
                                <option value="">— elige tu cuenta —</option>
                                <c:forEach var="a" items="${myAccounts}">
                                    <option value="${a.id}" ${a.id == selectedSourceId ? 'selected' : ''}>
                                        ${a.purpose} · ${a.accountNumber} · $ ${a.balance}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <noscript>
                        <button type="submit" class="auth-submit" style="width:auto; padding:0 var(--sp-3); margin-top: var(--sp-2);">
                            Continuar
                        </button>
                    </noscript>
                </form>
            </div>

            <%-- Step 2: only once a source is chosen. --%>
            <c:if test="${not empty selectedSourceId}">
                <div class="portal-card">
                    <h2 class="portal-card__title">2 · Para quién y cuánto</h2>

                    <c:choose>
                        <c:when test="${empty peers}">
                            <p class="portal-empty">
                                Ningún compañero tiene una cuenta con este propósito, así que no hay
                                destinos disponibles. Elige otra cuenta de origen.
                            </p>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/app/transferencia">
                                <input type="hidden" name="sourceId" value="${selectedSourceId}">

                                <div class="auth-field" style="margin-bottom: var(--sp-2);">
                                    <div class="auth-field__labelrow">
                                        <label class="auth-label" for="destId">Compañero</label>
                                    </div>
                                    <div class="auth-input-wrap">
                                        <select class="auth-input" style="padding-left: var(--sp-2);"
                                                id="destId" name="destId" required>
                                            <option value="">— elige —</option>
                                            <c:forEach var="p" items="${peers}">
                                                <option value="${p.accountId}">${p.label}</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>

                                <div class="auth-field" style="margin-bottom: var(--sp-2);">
                                    <div class="auth-field__labelrow">
                                        <label class="auth-label" for="amount">Monto (MXN)</label>
                                    </div>
                                    <div class="auth-input-wrap">
                                        <input class="auth-input" id="amount" name="amount"
                                               type="number" step="0.01" min="0.01"
                                               placeholder="0.00" required>
                                    </div>
                                </div>

                                <div class="auth-field" style="margin-bottom: var(--sp-3);">
                                    <div class="auth-field__labelrow">
                                        <label class="auth-label" for="description">Concepto (opcional)</label>
                                    </div>
                                    <div class="auth-input-wrap">
                                        <input class="auth-input" id="description" name="description"
                                               type="text" maxlength="200" placeholder="Ej. gasolina del viaje a Puebla">
                                    </div>
                                </div>

                                <button type="submit" class="auth-submit">Transferir</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>

        </c:otherwise>
    </c:choose>

</div>
</body>
</html>
