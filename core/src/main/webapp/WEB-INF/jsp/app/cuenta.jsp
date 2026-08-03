<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${account.purpose} · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal.css">
</head>
<body class="auth portal">
<div class="portal-wrap">

    <jsp:include page="nav.jsp"/>

    <a class="portal-back" href="${pageContext.request.contextPath}/app/home">← Mis cuentas</a>

    <h1 class="portal-title">${account.purpose}</h1>
    <p class="portal-sub">${account.accountNumber}</p>

    <div class="portal-total">
        <div class="portal-total__label">Saldo disponible</div>
        <div class="portal-total__value">$ ${account.balance} MXN</div>
    </div>

    <%-- Cards: access points to this account. Deleting one does NOT move money (RN-06). --%>
    <div class="portal-card">
        <h2 class="portal-card__title">Tarjetas de acceso</h2>
        <c:choose>
            <c:when test="${empty cards}">
                <p class="portal-empty">Esta cuenta no tiene tarjetas emitidas.</p>
            </c:when>
            <c:otherwise>
                <div class="portal-chips">
                    <c:forEach var="k" items="${cards}">
                        <div class="portal-chip ${k.status != 'ACTIVE' ? 'portal-chip--inactive' : ''}">
                            ${k.maskedPan}
                            <small>
                                <c:choose>
                                    <c:when test="${k.cardType == 'PHYSICAL'}">Física</c:when>
                                    <c:otherwise>Digital</c:otherwise>
                                </c:choose>
                                ·
                                <c:choose>
                                    <c:when test="${k.status == 'ACTIVE'}">Activa</c:when>
                                    <c:when test="${k.status == 'BLOCKED'}">Bloqueada</c:when>
                                    <c:otherwise>Inactiva</c:otherwise>
                                </c:choose>
                            </small>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- Immutable ledger for this account (HU-08). Newest first. --%>
    <div class="portal-card">
        <h2 class="portal-card__title">Historial de movimientos</h2>
        <c:choose>
            <c:when test="${empty movements}">
                <p class="portal-empty">Sin movimientos todavía.</p>
            </c:when>
            <c:otherwise>
                <table class="portal-table">
                    <tr>
                        <th>Fecha</th>
                        <th>Concepto</th>
                        <th>Descripción</th>
                        <th class="num">Monto</th>
                    </tr>
                    <c:forEach var="m" items="${movements}">
                        <tr>
                            <td>${m.createdAt}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${m.movementType == 'DEPOSIT'}">Depósito</c:when>
                                    <c:when test="${m.movementType == 'WITHDRAWAL'}">Retiro</c:when>
                                    <c:when test="${m.movementType == 'TRANSFER_IN'}">Transferencia recibida</c:when>
                                    <c:when test="${m.movementType == 'TRANSFER_OUT'}">Transferencia enviada</c:when>
                                    <c:when test="${m.movementType == 'REINTEGRATION'}">Reintegración</c:when>
                                    <c:otherwise>${m.movementType}</c:otherwise>
                                </c:choose>
                            </td>
                            <td>${m.description}</td>
                            <c:set var="incoming"
                                   value="${m.movementType == 'DEPOSIT' or m.movementType == 'TRANSFER_IN'}"/>
                            <td class="num ${incoming ? 'portal-in' : 'portal-out'}">
                                ${incoming ? '+' : '−'} $ ${m.amount}
                            </td>
                        </tr>
                    </c:forEach>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

</div>
</body>
</html>
