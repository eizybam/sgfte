package mx.sgfte.core.shared.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Publishes a per-deployment token that the shells append to the stylesheet URL.
 *
 * Why this exists: JSPs recompile on the fly, but browsers hold on to
 * assets/css/sgfte.css. While the screens are being rebuilt against the Figma
 * prototypes that combination is actively misleading — the new markup renders
 * while the new rules do not, so a correct screen looks broken and the fix
 * ("reload harder") is invisible from the page.
 *
 * The token changes on every startup, so a redeploy always lands on a URL the
 * browser has never seen. It does NOT change between requests, so the file
 * still caches normally for real users.
 */
@WebListener
public class AssetsVersionListener implements ServletContextListener {

    /** Read from the JSPs as ${applicationScope.assetsVersion}. */
    public static final String ATTRIBUTE = "assetsVersion";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // Base 36 sólo para que la cadena sea corta en la URL.
        String token = Long.toString(System.currentTimeMillis(), 36);
        event.getServletContext().setAttribute(ATTRIBUTE, token);
    }
}
