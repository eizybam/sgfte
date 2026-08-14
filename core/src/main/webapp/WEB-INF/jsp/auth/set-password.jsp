<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Crear/restablecer contraseña · misma página para las dos, porque el token
  ya dice de qué login se trata. Reutiliza exactamente el panel de login.jsp
  (auth.css), sin CSS nuevo.
--%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Crear contraseña · SGFTE</title>

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
                <h1 class="auth-title">Crea tu contraseña</h1>
                <p class="auth-subtitle">Es el último paso para entrar a tu portal de tarjetahabiente.</p>
            </div>

            <div class="auth-card">
                <c:choose>
                    <c:when test="${not valid}">
                        <div class="auth-alert auth-alert--error">
                            Este enlace ya no es válido — pudo vencer o ya haberse usado.
                        </div>
                        <a class="auth-forgot" style="display:block;margin-top:8px"
                           href="${pageContext.request.contextPath}/forgot-password">Pedir un enlace nuevo</a>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not empty error}">
                            <div class="auth-alert auth-alert--error">${fn:escapeXml(error)}</div>
                        </c:if>

                        <form id="set-password-form" method="post"
                              action="${pageContext.request.contextPath}/set-password" novalidate>
                            <input type="hidden" name="token" value="${fn:escapeXml(token)}">

                            <div class="auth-field" data-field="password">
                                <div class="auth-field__labelrow">
                                    <label class="auth-label" for="password">Nueva contraseña</label>
                                </div>
                                <div class="auth-input-wrap">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <rect x="4" y="11" width="16" height="10" rx="2"/>
                                        <path d="M8 11V7a4 4 0 0 1 8 0v4"/>
                                    </svg>
                                    <input class="auth-input" type="password" id="password" name="password"
                                           placeholder="Mínimo 8 caracteres" autocomplete="new-password"
                                           minlength="8" required>
                                </div>
                                <p class="auth-field__error">Debe tener al menos 8 caracteres.</p>
                            </div>

                            <div class="auth-field" data-field="confirmPassword">
                                <div class="auth-field__labelrow">
                                    <label class="auth-label" for="confirmPassword">Confirma tu contraseña</label>
                                </div>
                                <div class="auth-input-wrap">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <rect x="4" y="11" width="16" height="10" rx="2"/>
                                        <path d="M8 11V7a4 4 0 0 1 8 0v4"/>
                                    </svg>
                                    <input class="auth-input" type="password" id="confirmPassword" name="confirmPassword"
                                           placeholder="Repite tu contraseña" autocomplete="new-password" required>
                                </div>
                                <p class="auth-field__error">Debe coincidir con la de arriba.</p>
                            </div>

                            <button type="submit" class="auth-submit">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                                    <path d="m10 17 5-5-5-5"/>
                                    <path d="M15 12H3"/>
                                </svg>
                                Guardar y continuar
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<script>
    (function () {
        var form = document.getElementById("set-password-form");
        if (!form) return;

        function setError(fieldName, hasError) {
            var field = form.querySelector('[data-field="' + fieldName + '"]');
            var input = field.querySelector(".auth-input");
            field.classList.toggle("has-error", hasError);
            input.classList.toggle("is-invalid", hasError);
        }

        form.addEventListener("submit", function (e) {
            var pass = form.password.value;
            var confirm = form.confirmPassword.value;
            var passBad = pass.length < 8;
            var confirmBad = pass !== confirm;

            setError("password", passBad);
            setError("confirmPassword", confirmBad);

            if (passBad || confirmBad) {
                e.preventDefault();
                form.querySelector(".is-invalid").focus();
            }
        });

        ["password", "confirmPassword"].forEach(function (name) {
            form[name].addEventListener("input", function () {
                setError("password", false);
                setError("confirmPassword", false);
            });
        });
    })();
</script>
</body>
</html>
