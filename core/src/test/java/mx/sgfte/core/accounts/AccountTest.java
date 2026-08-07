package mx.sgfte.core.accounts;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    @Test
    void depositIncreasesBalance() {
        Account a = new Account(1L, 2L);
        a.deposit(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), a.getBalance());
    }

    @Test
    void cannotWithdrawMoreThanBalance() {
        Account a = new Account(1L, 2L);
        assertThrows(IllegalStateException.class,
                () -> a.withdraw(new BigDecimal("10.00")));
    }
}
