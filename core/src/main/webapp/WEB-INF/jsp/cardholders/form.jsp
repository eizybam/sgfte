<%--
  Created by IntelliJ IDEA.
  User: bam
  Date: 08/07/26
  Time: 7:56 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Alta de Tarjetahabiente · SGFTE</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-5" style="max-width: 640px;">

  <h1 class="h3 mb-1">Alta de Tarjetahabiente</h1>
  <p class="text-muted mb-4">Registra un nuevo empleado en el sistema SGFTE.</p>

  <c:if test="${not empty successId}">
    <div class="alert alert-success">
      Tarjetahabiente registrado con ID <strong>${successId}</strong>:
        ${cardholder.firstName} ${cardholder.lastName} (${cardholder.email}).
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

  <form method="post" action="${pageContext.request.contextPath}/cardholders"
        class="card p-4 shadow-sm border-0">
    <div class="mb-3">
      <label class="form-label">Nombre</label>
      <input name="firstName" class="form-control" value="${cardholder.firstName}" required>
    </div>
    <div class="mb-3">
      <label class="form-label">Apellido</label>
      <input name="lastName" class="form-control" value="${cardholder.lastName}" required>
    </div>
    <div class="mb-3">
      <label class="form-label">Correo</label>
      <input type="email" name="email" class="form-control" value="${cardholder.email}" required>
    </div>
    <div class="mb-4">
      <label class="form-label">Teléfono <span class="text-muted">(opcional)</span></label>
      <input name="phone" class="form-control" value="${cardholder.phone}">
    </div>
    <button class="btn btn-primary">Registrar tarjetahabiente</button>
  </form>

</div>
</body>
</html>
