package mx.sgfte.core.accounts;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    @Test
    void depositIncreasesBalance() {
        Account a = new Account("A-1", "Gasoline");
        a.deposit(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), a.getBalance());
    }

    @Test
    void cannotWithdrawMoreThanBalance() {
        Account a = new Account("A-1", "Gasoline");
        assertThrows(IllegalStateException.class,
                () -> a.withdraw(new BigDecimal("10.00")));
    }
}
