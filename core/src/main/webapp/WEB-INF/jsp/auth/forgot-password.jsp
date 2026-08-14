<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Recuperar contraseña · SGFTE</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap"
          rel="stylesheet">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body class="auth">
<div class="auth-page">

    <div class="auth-visual" role="presentation"></div>

    <div class="auth-panel">
        <div class="auth-glow" aria-hidden="true"></div>

        <div class="auth-content">
            <div class="auth-head">
                <p class="auth-brand">SGFTE</p>
                <h1 class="auth-title">Recupera tu contraseña</h1>
                <p class="auth-subtitle">Te mandamos un enlace para crear una nueva.</p>
            </div>

            <div class="auth-card">
                <c:choose>
                    <%-- sent=true en cuanto se procesó el POST — pase lo que pase
                         con el correo, incluso si no encontró a nadie. No hay
                         forma de distinguir desde aquí "sí existía" de "no
                         existía", que es justo el punto. --%>
                    <c:when test="${sent}">
                        <div class="auth-alert auth-alert--ok">
                            Si <strong>${fn:escapeXml(email)}</strong> tiene una cuenta, te mandamos un enlace para
                            restablecer tu contraseña. Revisa tu correo.
                        </div>
                        <a class="auth-forgot" style="display:block;margin-top:8px"
                           href="${pageContext.request.contextPath}/login">Volver a inicio de sesión</a>
                    </c:when>
                    <c:otherwise>
                        <form id="forgot-form" method="post"
                              action="${pageContext.request.contextPath}/forgot-password" novalidate>

                            <div class="auth-field" data-field="email">
                                <div class="auth-field__labelrow">
                                    <label class="auth-label" for="email">Email corporativo</label>
                                </div>
                                <div class="auth-input-wrap">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <rect x="2" y="4" width="20" height="16" rx="2"/>
                                        <path d="m2 7 10 6 10-6"/>
                                    </svg>
                                    <input class="auth-input" type="email" id="email" name="email"
                                           placeholder="tu@empresa.com" autocomplete="username" required>
                                </div>
                                <p class="auth-field__error">Ingresa un correo válido.</p>
                            </div>

                            <button type="submit" class="auth-submit">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                                    <path d="m10 17 5-5-5-5"/>
                                    <path d="M15 12H3"/>
                                </svg>
                                Enviar enlace
                            </button>
                        </form>

                        <a class="auth-forgot" style="display:block;margin-top:8px"
                           href="${pageContext.request.contextPath}/login">Volver a inicio de sesión</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script>
    (function () {
        var form = document.getElementById("forgot-form");
        if (!form) return;
        var emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        form.addEventListener("submit", function (e) {
            var field = form.querySelector('[data-field="email"]');
            var input = field.querySelector(".auth-input");
            var bad = !emailRe.test(form.email.value.trim());
            field.classList.toggle("has-error", bad);
            input.classList.toggle("is-invalid", bad);
            if (bad) { e.preventDefault(); input.focus(); }
        });

        form.email.addEventListener("input", function () {
            form.querySelector('[data-field="email"]').classList.remove("has-error");
            form.email.classList.remove("is-invalid");
        });
    })();
</script>
</body>
</html>