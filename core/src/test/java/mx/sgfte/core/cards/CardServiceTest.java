package mx.sgfte.core.cards;

import mx.sgfte.core.users.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Card issuing and invalidation (RF-04).
 *
 * The rule worth protecting here is RN-06: invalidating a card must NOT move
 * money. This class proves the card path never reaches an account balance —
 * CardService has no access to AccountDao at all, which is the structural reason
 * the rule cannot be broken by accident.
 */
class CardServiceTest {

    private static class FakeCardDao extends CardDao {
        boolean accountActive = true;
        boolean invalidateSucceeds = true;
        final List<Card> inserted = new ArrayList<>();
        /*
          Las tarjetas que la cuenta YA tiene. Vacía por defecto.

          Hay que redefinir findByAccount aunque el test no lo llame: si no, la
          llamada cae en el CardDao de verdad y el test abre una conexión a
          Oracle. Un test que necesita la base para pasar no es un test unitario,
          y falla por motivos que no tienen que ver con lo que comprueba.
         */
        final List<Card> existing = new ArrayList<>();

        @Override
        public boolean isAccountActive(long accountId) { return accountActive; }

        @Override
        public List<Card> findByAccount(long accountId) { return existing; }

        @Override
        public long insert(Card card) {
            inserted.add(card);
            return 99L;
        }

        @Override
        public boolean invalidate(long cardId) { return invalidateSucceeds; }
    }

    private final FakeCardDao dao = new FakeCardDao();
    private final CardService service = new CardService(dao);

    @Test
    void issuesAPhysicalCard() {
        assertEquals(99L, service.issue(1L, "PHYSICAL"));
        assertEquals("PHYSICAL", dao.inserted.get(0).getCardType());
    }

    @Test
    void issuesADigitalCard() {
        service.issue(1L, "DIGITAL");
        assertEquals("DIGITAL", dao.inserted.get(0).getCardType());
    }

    @Test
    void newCardsStartActive() {
        service.issue(1L, "PHYSICAL");
        assertEquals("ACTIVE", dao.inserted.get(0).getStatus());
    }

    /**
     * RNF-05: the system simulates cards, it never stores a real PAN.
     * Only the last four digits may ever be visible.
     */
    @Test
    void panIsMaskedAndKeepsOnlyFourDigits() {
        service.issue(1L, "PHYSICAL");
        String pan = dao.inserted.get(0).getMaskedPan();

        assertTrue(pan.startsWith("**** **** **** "), "PAN sin enmascarar: " + pan);
        assertEquals(19, pan.length());
        assertTrue(pan.substring(15).matches("\\d{4}"), "los últimos 4 deben ser dígitos");
    }

    @Test
    void accountIsRequired() {
        assertThrows(ValidationException.class, () -> service.issue(null, "PHYSICAL"));
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    void cardTypeMustBePhysicalOrDigital() {
        assertThrows(ValidationException.class, () -> service.issue(1L, "VIRTUAL"));
        assertThrows(ValidationException.class, () -> service.issue(1L, "physical"));
        assertThrows(ValidationException.class, () -> service.issue(1L, null));
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    void cannotIssueOnAnInactiveAccount() {
        dao.accountActive = false;
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.issue(1L, "PHYSICAL"));
        assertTrue(e.getErrors().get(0).contains("inactiva"));
        assertTrue(dao.inserted.isEmpty());
    }

    @Test
    void invalidatingAnActiveCardSucceeds() {
        assertDoesNotThrow(() -> service.invalidate(5L));
    }

    @Test
    void invalidatingAnAlreadyInactiveCardIsReported() {
        dao.invalidateSucceeds = false;
        assertThrows(ValidationException.class, () -> service.invalidate(5L));
    }

    // ---- Una tarjeta activa de cada tipo por cuenta (V6) --------------------

    @Test
    void rejectsASecondActiveCardOfTheSameType() {
        dao.existing.add(new Card(1L, "PHYSICAL", "**** **** **** 1111"));

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.issue(1L, "PHYSICAL"));

        assertTrue(e.getErrors().get(0).contains("ya tiene una tarjeta física activa"));
        assertTrue(dao.inserted.isEmpty(), "no debe insertarse nada");
    }

    @Test
    void allowsTheOtherTypeWhenOneAlreadyExists() {
        dao.existing.add(new Card(1L, "PHYSICAL", "**** **** **** 1111"));

        service.issue(1L, "DIGITAL");

        assertEquals("DIGITAL", dao.inserted.get(0).getCardType());
    }

    /** Perder una tarjeta y reponerla tiene que seguir siendo posible. */
    @Test
    void allowsReissuingWhenThePreviousOneWasInvalidated() {
        Card cancelled = new Card(1L, "PHYSICAL", "**** **** **** 1111");
        cancelled.setStatus("INACTIVE");
        dao.existing.add(cancelled);

        service.issue(1L, "PHYSICAL");

        assertEquals("PHYSICAL", dao.inserted.get(0).getCardType());
    }
}
