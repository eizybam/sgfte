package mx.sgfte.core.cards;

import mx.sgfte.core.users.ValidationException;

import java.security.SecureRandom;
import java.util.List;

/** Card logic: issue a card for an account, invalidate a card. */
public class CardService {

    /**
     * Vigencia de una tarjeta nueva, en años.
     *
     * Es una convención de la empresa, no una ley, así que vive aquí y no
     * repartida por las consultas: cambiarla es cambiar esta línea.
     */
    public static final int VALIDITY_YEARS = 4;

    private static final SecureRandom RANDOM = new SecureRandom();
    private final CardDao dao;

    public CardService() { this(new CardDao()); }
    public CardService(CardDao dao) { this.dao = dao; }

    /**
     * Issues a card. Validates the type and that the account is active,
     * generates a masked PAN (we never store a real card number in this system).
     */
    public long issue(Long accountId, String cardType) {
        if (accountId == null) {
            throw new ValidationException(List.of("Debes elegir una cuenta"));
        }
        if (!"PHYSICAL".equals(cardType) && !"DIGITAL".equals(cardType)) {
            throw new ValidationException(List.of("Tipo de tarjeta inválido"));
        }
        if (!dao.isAccountActive(accountId)) {
            throw new ValidationException(List.of("La cuenta no existe o está inactiva"));
        }
        /*
          Una cuenta tiene como mucho una tarjeta activa de cada tipo. Quien lo
          garantiza de verdad es el índice uq_card_active_type (V6); esto está
          aquí para que el admin lea por qué no se pudo en vez de un ORA-00001.
         */
        boolean repeated = dao.findByAccount(accountId).stream()
                .anyMatch(c -> cardType.equals(c.getCardType()) && "ACTIVE".equals(c.getStatus()));
        if (repeated) {
            throw new ValidationException(List.of(
                    "Esta cuenta ya tiene una tarjeta "
                    + ("PHYSICAL".equals(cardType) ? "física" : "digital")
                    + " activa. Invalida la actual antes de expedir otra."));
        }
        Card card = new Card(accountId, cardType, generateMaskedPan());
        card.setExpiresAt(java.time.LocalDate.now().plusYears(VALIDITY_YEARS));
        return dao.insert(card);
    }

    public void invalidate(long cardId) {
        if (!dao.invalidate(cardId)) {
            throw new ValidationException(List.of("La tarjeta no existe o ya estaba inactiva"));
        }
    }

    /**
     * Bloqueo temporal: la tarjeta deja de pagar pero sigue existiendo.
     *
     * Es el término medio que faltaba entre "activa" e "invalidada". Perder una
     * tarjeta el martes y encontrarla el jueves no debería costar un plástico
     * nuevo con otro PAN.
     */
    public void block(long cardId) {
        if (!dao.block(cardId)) {
            throw new ValidationException(List.of("La tarjeta no está activa."));
        }
    }

    public void unblock(long cardId) {
        if (!dao.unblock(cardId)) {
            throw new ValidationException(List.of("La tarjeta no está bloqueada."));
        }
    }

    public List<Card> cardsOf(long accountId) {
        return dao.findByAccount(accountId);
    }

    /** "**** **** **** 4821" — only the last 4 digits are shown. */
    private String generateMaskedPan() {
        int last4 = RANDOM.nextInt(10000);
        return String.format("**** **** **** %04d", last4);
    }
}
