<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Panel · SGFTE</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
    <style>
        .home-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: var(--sp-6); }
        .home-card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border);
            border-radius: var(--sgfte-radius-card); padding: var(--sp-5); max-width: 520px; width: 100%; }
        .home-card h1 { font-family: var(--sgfte-font-title); font-weight: 600; color: var(--sgfte-white);
            margin: 0 0 var(--sp-1); font-size: 28px; }
        .home-card p { color: var(--sgfte-tan); margin: 0 0 var(--sp-4); }
        .home-role { font-family: var(--sgfte-font-mono); font-size: 13px; letter-spacing: 0.6px;
            text-transform: uppercase; color: var(--sgfte-salmon); }
    </style>
</head>
<body class="auth">
<div class="home-wrap">
    <div class="home-card">
        <h1>Bienvenido, ${sessionScope.user.fullName}</h1>
        <p class="home-role">Rol: ${sessionScope.user.role}</p>
        <p>Este es un panel provisional. Las vistas de administración se conectarán aquí.</p>
        <form method="post" action="${pageContext.request.contextPath}/logout">
            <button type="submit" class="auth-submit" style="width:auto; padding:0 var(--sp-3);">
                Cerrar sesión
            </button>
        </form>
    </div>
</div>
</body>
</html>
