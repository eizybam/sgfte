<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Transferencia · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .wrap { max-width: 560px; margin: 0 auto; padding: var(--sp-6) var(--sp-3); }
        .card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-5); }
        h1 { font-family: var(--sgfte-font-title); color: var(--sgfte-white); font-size: 28px; margin: 0 0 var(--sp-1); }
        .hint { font-family: var(--sgfte-font-mono); font-size: 12px; letter-spacing: .6px; text-transform: uppercase; color: var(--sgfte-tan); margin: 0 0 var(--sp-4); }
    </style>
</head>
<body class="auth">
<div class="wrap">
    <div class="card">
        <h1>Transferencia P2P</h1>
        <p class="hint">Solo entre cuentas del mismo propósito</p>

        <c:if test="${not empty success}"><div class="auth-alert auth-alert--ok">${success}</div></c:if>
        <c:if test="${not empty errors}">
            <div class="auth-alert auth-alert--error"><c:forEach var="e" items="${errors}">${e}<br></c:forEach></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/admin/transferencia">
            <div class="auth-field">
                <div class="auth-field__labelrow"><label class="auth-label" for="sourceId">Cuenta origen</label></div>
                <div class="auth-input-wrap">
                    <select class="auth-input" style="padding-left: var(--sp-2);" id="sourceId" name="sourceId" required>
                        <option value="">— elige —</option>
                        <c:forEach var="a" items="${accounts}"><option value="${a.id}">${a.label} ($ ${a.balance})</option></c:forEach>
                    </select>
                </div>
            </div>
            <div class="auth-field">
                <div class="auth-field__labelrow"><label class="auth-label" for="destId">Cuenta destino</label></div>
                <div class="auth-input-wrap">
                    <select class="auth-input" style="padding-left: var(--sp-2);" id="destId" name="destId" required>
                        <option value="">— elige —</option>
                        <c:forEach var="a" items="${accounts}"><option value="${a.id}">${a.label} ($ ${a.balance})</option></c:forEach>
                    </select>
                </div>
            </div>
            <div class="auth-field">
                <div class="auth-field__labelrow"><label class="auth-label" for="amount">Monto (MXN)</label></div>
                <div class="auth-input-wrap">
                    <input class="auth-input" style="padding-left: var(--sp-2);" type="number" step="0.01" min="0.01" id="amount" name="amount" placeholder="200.00" required>
                </div>
            </div>
            <button type="submit" class="auth-submit">Transferir</button>
        </form>
    </div>
</div>
</body>
</html>
