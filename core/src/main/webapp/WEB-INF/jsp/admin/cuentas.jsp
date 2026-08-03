<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Gestión de cuentas · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .wrap { max-width: 720px; margin: 0 auto; padding: var(--sp-6) var(--sp-3); }
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
        <h1>Gestión de cuentas</h1>
        <c:if test="${not empty success}"><div class="auth-alert auth-alert--ok">${success}</div></c:if>
        <c:if test="${not empty errors}"><div class="auth-alert auth-alert--error"><c:forEach var="e" items="${errors}">${e}<br></c:forEach></div></c:if>

        <table>
            <tr><th>ID</th><th>Cuenta</th><th>Saldo</th><th></th></tr>
            <c:forEach var="a" items="${accounts}">
                <tr>
                    <td>${a.id}</td>
                    <td>${a.label}</td>
                    <td>$ ${a.balance}</td>
                    <td>
                        <form method="post" action="${pageContext.request.contextPath}/admin/cuentas" style="margin:0;"
                              onsubmit="return confirm('¿Eliminar la cuenta? El saldo se reintegra a la Concentradora.');">
                            <input type="hidden" name="accountId" value="${a.id}">
                            <button type="submit" class="del">Eliminar</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty accounts}"><tr><td colspan="4">No hay cuentas activas.</td></tr></c:if>
        </table>
    </div>
</div>
</body>
</html>
