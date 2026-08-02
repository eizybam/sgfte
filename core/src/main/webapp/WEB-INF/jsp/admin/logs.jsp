<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Logs · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .wrap { max-width: 860px; margin: 0 auto; padding: var(--sp-6) var(--sp-3); }
        .card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-5); }
        h1 { font-family: var(--sgfte-font-title); color: var(--sgfte-white); font-size: 28px; margin: 0 0 var(--sp-3); }
        table { width: 100%; border-collapse: collapse; }
        td, th { text-align: left; padding: var(--sp-1) var(--sp-1) var(--sp-1) 0; border-bottom: 1px solid var(--sgfte-border); font-size: 13px; }
        th { font-family: var(--sgfte-font-mono); font-size: 12px; text-transform: uppercase; color: var(--sgfte-tan); }
        .type { font-family: var(--sgfte-font-mono); color: var(--sgfte-salmon); }
    </style>
</head>
<body class="auth">
<div class="wrap">
    <div class="card">
        <h1>Bitácora del sistema (Logs)</h1>
        <table>
            <tr><th>ID</th><th>Fecha</th><th>Evento</th><th>Detalle</th><th>Actor</th></tr>
            <c:forEach var="l" items="${logs}">
                <tr>
                    <td>${l.id}</td>
                    <td>${l.createdAt}</td>
                    <td class="type">${l.eventType}</td>
                    <td>${l.detail}</td>
                    <td>${l.actor}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty logs}"><tr><td colspan="5">Sin eventos registrados.</td></tr></c:if>
        </table>
    </div>
</div>
</body>
</html>
