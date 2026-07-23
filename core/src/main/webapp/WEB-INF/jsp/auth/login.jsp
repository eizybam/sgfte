<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Iniciar sesión · SGFTE</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap"
          rel="stylesheet">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body class="auth">
<div class="auth-page">

    <%-- Left: corporate building image (see NOTES.md to export assets/img/login-bg.jpg) --%>
    <div class="auth-visual" role="presentation"></div>

    <%-- Right: form panel --%>
    <div class="auth-panel">
        <div class="auth-glow" aria-hidden="true"></div>

        <div class="auth-content">
            <div class="auth-head">
                <p class="auth-brand">SGFTE</p>
                <h1 class="auth-title">Inicio de sesión</h1>
                <p class="auth-subtitle">Accede al dashboard empresarial y gestiona tus cuentas, fondos y tarjetas.</p>
            </div>

            <div class="auth-card">

                <%-- Backend populates these later (LoginServlet sets request attributes). --%>
                <c:if test="${not empty error}">
                    <div class="auth-alert auth-alert--error">${error}</div>
                </c:if>
                <c:if test="${not empty success}">
                    <div class="auth-alert auth-alert--ok">${success}</div>
                </c:if>

                <form id="login-form" method="post"
                      action="${pageContext.request.contextPath}/login" novalidate>

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
                                   placeholder="admin@empresa.com" autocomplete="username"
                                   value="${fn:escapeXml(email)}" required>
                        </div>
                        <p class="auth-field__error">Ingresa un correo válido.</p>
                    </div>

                    <div class="auth-field" data-field="password">
                        <div class="auth-field__labelrow">
                            <label class="auth-label" for="password">Contraseña</label>
                        </div>
                        <div class="auth-input-wrap">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="4" y="11" width="16" height="10" rx="2"/>
                                <path d="M8 11V7a4 4 0 0 1 8 0v4"/>
                            </svg>
                            <input class="auth-input" type="password" id="password" name="password"
                                   placeholder="••••••••••••" autocomplete="current-password" required>
                        </div>
                        <p class="auth-field__error">Ingresa tu contraseña.</p>
                    </div>

                    <%--
                        Forgot-password hook for teammates:
                        this button is where the "¿Olvidaste tu contraseña?" modal opens.
                        Wire your popup to #forgot-password-link (e.g. open a dialog, or
                        navigate to /forgot-password). Left as a plain button on purpose.
                    --%>
                    <button type="button" class="auth-forgot" id="forgot-password-link">
                        ¿Olvidaste tu contraseña?
                    </button>

                    <button type="submit" class="auth-submit">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                            <path d="m10 17 5-5-5-5"/>
                            <path d="M15 12H3"/>
                        </svg>
                        Acceder
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
    // Lightweight client-side validation. The server MUST still validate —
    // this is only for fast feedback (see the account slice discussion).
    (function () {
        var form = document.getElementById("login-form");
        var emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        function setError(fieldName, hasError) {
            var field = form.querySelector('[data-field="' + fieldName + '"]');
            var input = field.querySelector(".auth-input");
            field.classList.toggle("has-error", hasError);
            input.classList.toggle("is-invalid", hasError);
        }

        form.addEventListener("submit", function (e) {
            var email = form.email.value.trim();
            var pass = form.password.value;
            var emailBad = !emailRe.test(email);
            var passBad = pass.length === 0;

            setError("email", emailBad);
            setError("password", passBad);

            if (emailBad || passBad) {
                e.preventDefault();
                form.querySelector(".is-invalid").focus();
            }
        });

        // Clear a field's error as soon as the user corrects it.
        ["email", "password"].forEach(function (name) {
            form[name].addEventListener("input", function () { setError(name, false); });
        });
    })();
</script>
</body>
</html>
