<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Expedición de tarjetas · SGFTE</title>
  <link href="https://fonts.googleapis.com/css2?family=Hanken+Grotesk:wght@400;600&family=Manrope:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
  <style>
    .wrap { max-width: 620px; margin: 0 auto; padding: var(--sp-6) var(--sp-3); }
    .card { background: var(--sgfte-card); border: 1px solid var(--sgfte-border); border-radius: var(--sgfte-radius-card); padding: var(--sp-5); margin-bottom: var(--sp-4); }
    h1 { font-family: var(--sgfte-font-title); color: var(--sgfte-white); font-size: 28px; margin: 0 0 var(--sp-3); }
    table { width: 100%; border-collapse: collapse; }
    td, th { text-align: left; padding: var(--sp-1) 0; border-bottom: 1px solid var(--sgfte-border); font-size: 14px; }
    th { font-family: var(--sgfte-font-mono); font-size: 12px; text-transform: uppercase; color: var(--sgfte-tan); }
    .pan { font-family: var(--sgfte-font-mono); color: var(--sgfte-cream); }
  </style>
</head>
<body class="auth">
<div class="wrap">
  <div class="card">
    <h1>Expedición de tarjetas</h1>

    <c:if test="${not empty success}"><div class="auth-alert auth-alert--ok">${success}</div></c:if>
    <c:if test="${not empty errors}">
      <div class="auth-alert auth-alert--error">
        <c:forEach var="e" items="${errors}">${e}<br></c:forEach>
      </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/admin/cards">
      <input type="hidden" name="action" value="issue">
      <div class="auth-field">
        <div class="auth-field__labelrow"><label class="auth-label" for="accountId">Cuenta</label></div>
        <div class="auth-input-wrap">
          <select class="auth-input" style="padding-left: var(--sp-2);" id="accountId" name="accountId" required>
            <option value="">— elige —</option>
            <c:forEach var="a" items="${accounts}">
              <option value="${a.id}" ${a.id == selectedAccountId ? 'selected' : ''}>${a.label}</option>
            </c:forEach>
          </select>
        </div>
      </div>
      <div class="auth-field">
        <div class="auth-field__labelrow"><label class="auth-label" for="cardType">Tipo</label></div>
        <div class="auth-input-wrap">
          <select class="auth-input" style="padding-left: var(--sp-2);" id="cardType" name="cardType" required>
            <option value="PHYSICAL">Física</option>
            <option value="DIGITAL">Digital</option>
          </select>
        </div>
      </div>
      <button type="submit" class="auth-submit">Expedir tarjeta</button>
    </form>
  </div>

  <c:if test="${not empty cards}">
    <div class="card">
      <h1 style="font-size:20px;">Tarjetas de la cuenta</h1>
      <table>
        <tr><th>ID</th><th>Tipo</th><th>PAN</th><th>Estado</th><th></th></tr>
        <c:forEach var="cd" items="${cards}">
          <tr>
            <td>${cd.id}</td>
            <td>${cd.cardType}</td>
            <td class="pan">${cd.maskedPan}</td>
            <td>${cd.status}</td>
            <td>
              <c:if test="${cd.status == 'ACTIVE'}">
                <form method="post" action="${pageContext.request.contextPath}/admin/cards" style="margin:0;">
                  <input type="hidden" name="action" value="invalidate">
                  <input type="hidden" name="cardId" value="${cd.id}">
                  <input type="hidden" name="accountId" value="${selectedAccountId}">
                  <button type="submit" class="auth-forgot" style="margin:0;">Invalidar</button>
                </form>
              </c:if>
            </td>
          </tr>
        </c:forEach>
      </table>
    </div>
  </c:if>
</div>
</body>
</html>
