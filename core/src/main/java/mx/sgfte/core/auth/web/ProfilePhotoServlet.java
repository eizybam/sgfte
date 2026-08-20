package mx.sgfte.core.auth.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.auth.SessionUser;
import mx.sgfte.core.auth.UserDao;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * La foto de perfil: subirla (POST) y servirla (GET).
 *
 * UNA clase con DOS mapeos, igual que SettingsServlet y por lo mismo: cada área
 * vive detrás de su propio filtro —AuthFilter para /admin/*, AppAuthFilter para
 * /app/*— y una URL fuera de las dos no la protegería ninguno.
 *
 * NO recibe ningún id. Ni en la ruta ni como parámetro: el usuario sale de la
 * sesión, así que la única foto que este endpoint puede leer o escribir es la
 * de quien pide. Es la regla de propiedad de /app llevada al extremo — no hay
 * un `if` que se pueda olvidar ni un WHERE que se pueda escribir mal, porque no
 * hay nada que manipular. La URL de la foto de otra persona no existe.
 *
 * Va aparte de SettingsServlet, y no como un doPost más, porque este formulario
 * es multipart/form-data y el de la contraseña no: sobre un cuerpo multipart,
 * getParameter() devuelve null hasta que el servlet declara @MultipartConfig.
 * Separándolos, el POST de contraseña queda exactamente como estaba.
 */
@WebServlet({"/admin/ajustes/foto", "/app/ajustes/foto"})
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 2 * 1024 * 1024 + 8192)
public class ProfilePhotoServlet extends HttpServlet {

    /*
      Los mismos tres del CHECK de la base. Se validan aquí para poder dar un
      mensaje en español, y en la base para que no dependa de que se validen
      aquí. Lo que se guarda es lo que después sale como Content-Type.
     */
    private static final Set<String> ALLOWED = Set.of("image/png", "image/jpeg", "image/webp");

    private final UserDao users = new UserDao();
    private final AuditLogService audit = new AuditLogService();

    /** Sirve la foto de quien pide. Sin parámetros: no hay otra que pueda servir. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SessionUser user = (SessionUser) req.getSession().getAttribute("user");

        Optional<UserDao.ProfilePhoto> found = users.findPhoto(user.getId());
        if (found.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        UserDao.ProfilePhoto photo = found.get();
        resp.setContentType(photo.contentType());
        resp.setContentLength(photo.bytes().length);
        /*
          Sin caché. Si el navegador guarda la imagen, al subir una nueva sigue
          enseñando la anterior y parece que la subida falló — el usuario la
          vuelve a subir dos o tres veces. Es una imagen, en una sola pantalla:
          no hay nada que ahorrar.
         */
        resp.setHeader("Cache-Control", "no-store");
        resp.getOutputStream().write(photo.bytes());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        HttpSession session = req.getSession();
        SessionUser user = (SessionUser) session.getAttribute("user");

        try {
            Part part = req.getPart("photo");

            if (part == null || part.getSize() == 0) {
                session.setAttribute("photoErrors", List.of("Elige una imagen antes de guardar."));
            } else if (!ALLOWED.contains(part.getContentType())) {
                session.setAttribute("photoErrors", List.of("Formato no admitido. Usa PNG, JPG o WEBP."));
            } else {
                byte[] bytes;
                try (InputStream in = part.getInputStream()) {
                    bytes = in.readAllBytes();
                }
                users.updatePhoto(user.getId(), bytes, part.getContentType());
                audit.record(AuditEvent.PROFILE_PHOTO_UPDATED, "Usuario " + user.getEmail(), req);
            }
        } catch (IllegalStateException e) {
            /*
              Lo lanza Tomcat al pasar maxFileSize, ANTES de que lleguemos a
              mirar el archivo. No es un fallo del servidor: es alguien subiendo
              la foto de 8 MB que le sacó el celular. Sin este catch sale un 500
              y parece que la aplicación se rompió.
             */
            session.setAttribute("photoErrors", List.of("La imagen no puede pesar más de 2 MB."));
        }

        // Post/redirect/get: un F5 después de subir no reenvía la imagen.
        resp.sendRedirect(req.getContextPath() + backTo(req));
    }

    /**
     * De vuelta a Ajustes, al área por la que entró.
     *
     * Literales, no una URL construida a partir de la petición — la misma regla
     * que PortalPurchaseServlet.backTo().
     */
    private String backTo(HttpServletRequest req) {
        return req.getServletPath().startsWith("/admin") ? "/admin/ajustes" : "/app/ajustes";
    }
}
