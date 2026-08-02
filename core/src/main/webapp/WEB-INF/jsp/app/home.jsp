<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Mi cuenta · SGFTE</title>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: var(--sp-6); }
        .card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-5); max-width: 520px; width: 100%; }
        h1 { font-family: var(--sgfte-font-title); color: var(--sgfte-white); font-size: 26px; margin: 0 0 var(--sp-2); }
        p { color: var(--sgfte-tan); }
    </style>
</head>
<body class="auth">
<div class="wrap">
    <div class="card">
        <h1>Bienvenido, ${sessionScope.user.fullName}</h1>
        <p>Área del tarjetahabiente. Aquí verás tus cuentas, tarjetas, transferencias e historial.</p>
        <form method="post" action="${pageContext.request.contextPath}/logout">
            <button type="submit" class="auth-submit" style="width:auto; padding:0 var(--sp-3);">Cerrar sesión</button>
        </form>
    </div>
</div>
</body>
</html>
