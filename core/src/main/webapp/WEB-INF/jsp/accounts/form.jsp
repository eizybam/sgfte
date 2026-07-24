<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Crear Cuenta · SGFTE</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-5" style="max-width: 640px;">

  <h1 class="h3 mb-1">Crear Cuenta</h1>
  <p class="text-muted mb-4">Asigna una cuenta con propósito a un tarjetahabiente. El saldo inicia en $0.00 MXN.</p>

  <c:if test="${not empty successId}">
    <div class="alert alert-success">
      Cuenta creada: <strong>${successNumber}</strong>
      (ID interno ${successId}, saldo inicial $0.00 MXN).
    </div>
  </c:if>

  <c:if test="${not empty errors}">
    <div class="alert alert-danger">
      <strong>Revisa los datos:</strong>
      <ul class="mb-0">
        <c:forEach var="e" items="${errors}"><li>${e}</li></c:forEach>
      </ul>
    </div>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/accounts"
        class="card p-4 shadow-sm border-0">
    <div class="mb-3">
      <label class="form-label">Tarjetahabiente</label>
      <select name="cardholderId" class="form-select" required>
        <option value="">— elige —</option>
        <c:forEach var="ch" items="${cardholders}">
          <option value="${ch.id}" ${ch.id == selectedCardholderId ? 'selected' : ''}>
              ${ch.lastName}, ${ch.firstName}
          </option>
        </c:forEach>
      </select>
    </div>
    <div class="mb-4">
      <label class="form-label">Propósito</label>
      <select name="categoryId" class="form-select" required>
        <option value="">— elige —</option>
        <c:forEach var="cat" items="${categories}">
          <option value="${cat.id}" ${cat.id == selectedCategoryId ? 'selected' : ''}>
              ${cat.name}
          </option>
        </c:forEach>
      </select>
    </div>
    <button class="btn btn-primary">Crear cuenta</button>
  </form>

</div>
</body>
</html>
