<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Raíz de la aplicación. No tiene contenido propio: manda a /login, y desde ahí
  LoginServlet decide el área según el rol (admin o tarjetahabiente).

  Antes aquí vivía el "Hello World" del arquetipo de IntelliJ, que era lo primero
  que veía cualquiera que abriera la URL base del sistema.
--%>
<c:redirect url="/login"/>
