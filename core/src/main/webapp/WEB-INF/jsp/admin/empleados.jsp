<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Empleados · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .wrap { max-width: 640px; margin: 0 auto; padding: var(--sp-6) var(--sp-3); }
        .card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-5); }
        h1 { font-family: var(--sgfte-font-title); color: var(--sgfte-white); font-size: 28px; margin: 0 0 var(--sp-3); }
        table { width: 100%; border-collapse: collapse; }
        td, th { text-align: left; padding: var(--sp-1) 0; border-bottom: 1px solid var(--sgfte-border); font-size: 14px; }
        th { font-family: var(--sgfte-font-mono); font-size: 12px; text-transform: uppercase; color: var(--sgfte-tan); }
        .del { color: var(--sgfte-error-text); background: none; border: 0; cursor: pointer; font-size: 14px; }
    </style>
</head>
<body class="auth">
<div class="wrap">
    <div class="card">
        <h1>Empleados (tarjetahabientes)</h1>
        <c:if test="${not empty success}"><div class="auth-alert auth-alert--ok">${success}</div></c:if>
        <c:if test="${not empty errors}"><div class="auth-alert auth-alert--error"><c:forEach var="e" items="${errors}">${e}<br></c:forEach></div></c:if>

        <table>
            <tr><th>ID</th><th>Nombre</th><th></th></tr>
            <c:forEach var="ch" items="${cardholders}">
                <tr>
                    <td>${ch.id}</td>
                    <td>${ch.fullName}</td>
                    <td>
                        <form method="post" action="${pageContext.request.contextPath}/admin/empleados" style="margin:0;"
                              onsubmit="return confirm('¿Eliminar al tarjetahabiente? Se reintegran todos sus saldos y se invalidan sus tarjetas.');">
                            <input type="hidden" name="cardholderId" value="${ch.id}">
                            <button type="submit" class="del">Eliminar</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty cardholders}"><tr><td colspan="3">No hay tarjetahabientes activos.</td></tr></c:if>
        </table>
    </div>
</div>
</body>
</html>
