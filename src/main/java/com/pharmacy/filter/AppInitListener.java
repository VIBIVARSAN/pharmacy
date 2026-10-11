package com.pharmacy.filter;

import com.pharmacy.service.AppContext;
import com.pharmacy.util.AppConfig;
import com.pharmacy.util.PooledConnectionProvider;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;

import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Starts the application: builds the connection pool and services, and hardens the session cookie. */
public class AppInitListener implements ServletContextListener {
    public static final String APP_CONTEXT = "appContext";
    public static final String STARTUP_ERROR = "startupError";
    private static final Logger LOG = Logger.getLogger(AppInitListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();

        SessionCookieConfig cookie = ctx.getSessionCookieConfig();
        cookie.setHttpOnly(true);
        cookie.setSecure(AppConfig.getBoolean("COOKIE_SECURE", false));
        cookie.setAttribute("SameSite", "Lax");
        // Session ids travel only in the cookie, never in URLs.
        ctx.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));

        try {
            AppContext app = new AppContext(PooledConnectionProvider.fromConfig());
            ctx.setAttribute(APP_CONTEXT, app);
            app.getAuthService().announceSetupTokenIfNeeded();
            LOG.info("Pharmacy Management System started.");
        } catch (RuntimeException e) {
            // Keep the web app deployable so the browser can show a helpful configuration page.
            ctx.setAttribute(STARTUP_ERROR, e.getMessage());
            LOG.log(Level.SEVERE, "Application could not start: " + e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object app = event.getServletContext().getAttribute(APP_CONTEXT);
        if (app instanceof AppContext context) {
            context.close();
        }
    }
}
