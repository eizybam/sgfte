<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Historial · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .wrap { max-width: 720px; margin: 0 auto; padding: var(--sp-6) var(--sp-3); }
        .card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-5); margin-bottom: var(--sp-4); }
        h1 { font-family: var(--sgfte-font-title); color: var(--sgfte-white); font-size: 28px; margin: 0 0 var(--sp-3); }
        table { width: 100%; border-collapse: collapse; }
        td, th { text-align: left; padding: var(--sp-1) var(--sp-1) var(--sp-1) 0; border-bottom: 1px solid var(--sgfte-border); font-size: 14px; }
        th { font-family: var(--sgfte-font-mono); font-size: 12px; text-transform: uppercase; color: var(--sgfte-tan); }
        .amt { font-family: var(--sgfte-font-mono); color: var(--sgfte-cream); text-align: right; }
    </style>
</head>
<body class="auth">
<div class="wrap">
    <div class="card">
        <h1>Historial de movimientos</h1>
        <form method="get" action="${pageContext.request.contextPath}/admin/historial">
            <div class="auth-field" style="margin-bottom: var(--sp-2);">
                <div class="auth-field__labelrow"><label class="auth-label" for="accountId">Cuenta</label></div>
                <div class="auth-input-wrap">
                    <select class="auth-input" style="padding-left: var(--sp-2);" id="accountId" name="accountId" onchange="this.form.submit()">
                        <option value="">— elige —</option>
                        <c:forEach var="a" items="${accounts}">
                            <option value="${a.id}" ${a.id == selectedAccountId ? 'selected' : ''}>${a.label}</option>
                        </c:forEach>
                    </select>
                </div>
            </div>
        </form>
    </div>

    <c:if test="${not empty selectedAccountId}">
        <div class="card">
            <table>
                <tr><th>Fecha</th><th>Tipo</th><th>Descripción</th><th class="amt">Monto</th></tr>
                <c:forEach var="m" items="${movements}">
                    <tr>
                        <td>${m.createdAt}</td>
                        <td>${m.movementType}</td>
                        <td>${m.description}</td>
                        <td class="amt">$ ${m.amount}</td>
                    </tr>
                </c:forEach>
                <c:if test="${empty movements}">
                    <tr><td colspan="4">Sin movimientos.</td></tr>
                </c:if>
            </table>
        </div>
    </c:if>
</div>
</body>
</html>
