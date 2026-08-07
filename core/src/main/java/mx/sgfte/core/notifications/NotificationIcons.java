package mx.sgfte.core.notifications;

import mx.sgfte.core.audit.AuditEvent;

/**
 * Which icon a notification row gets, chosen from its event type.
 *
 * Same idea as portal.PortalIcons: kept in Java and not a chain of c:choose in
 * the JSP, because a defensive default matters here too — an event_type value
 * this build does not recognise (an older row, after AuditEvent changes) must
 * not blow up the page, so it falls back to the neutral bell instead.
 */
final class NotificationIcons {

    private NotificationIcons() {}

    static final String FALLBACK = "i-bell";

    static String forEvent(String eventType) {
        AuditEvent event;
        try {
            event = AuditEvent.valueOf(eventType);
        } catch (IllegalArgumentException | NullPointerException e) {
            return FALLBACK;
        }
        return switch (event) {
            case LOGIN_OK, LOGIN_FAILED, LOGOUT -> "i-shield";
            case CARD_ISSUED, CARD_INVALIDATED -> "i-cards";
            case ACCOUNT_CREATED, ACCOUNT_DELETED,
                 CARDHOLDER_CREATED, CARDHOLDER_DELETED -> "i-people";
            case TRANSFER, DISPERSION, DISPERSION_REJECTED, CONCENTRATOR_FUNDED -> "i-money";
            case CATEGORY_CREATED, CATEGORY_RETIRED, CATEGORY_ACTIVATED -> "i-categories";
            default -> FALLBACK;
        };
    }
}
