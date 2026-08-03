<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Mis cuentas · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal.css">
</head>
<body class="auth portal">
<div class="portal-wrap">

    <jsp:include page="nav.jsp"/>

    <h1 class="portal-title">Mis cuentas</h1>
    <p class="portal-sub">Cada cuenta tiene un propósito y su propio saldo. El dinero vive en la cuenta, no en la tarjeta.</p>

    <div class="portal-total">
        <div class="portal-total__label">Saldo total disponible</div>
        <div class="portal-total__value">$ ${total} MXN</div>
    </div>

    <c:choose>
        <c:when test="${empty accounts}">
            <div class="portal-card">
                <p class="portal-empty">
                    Todavía no tienes cuentas asignadas. Cuando tu administrador te asigne una,
                    aparecerá aquí con su propósito y su saldo.
                </p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="portal-grid">
                <c:forEach var="a" items="${accounts}">
                    <a class="portal-account"
                       href="${pageContext.request.contextPath}/app/cuenta?id=${a.id}">
                        <div class="portal-account__purpose">${a.purpose}</div>
                        <div class="portal-account__number">${a.accountNumber}</div>
                        <div class="portal-account__balance">$ ${a.balance}</div>
                        <div class="portal-account__cards">
                            <c:choose>
                                <c:when test="${a.activeCards == 0}">Sin tarjetas activas</c:when>
                                <c:when test="${a.activeCards == 1}">1 tarjeta activa</c:when>
                                <c:otherwise>${a.activeCards} tarjetas activas</c:otherwise>
                            </c:choose>
                        </div>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>

</div>
</body>
</html>
