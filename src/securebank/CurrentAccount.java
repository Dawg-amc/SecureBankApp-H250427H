package securebank;

import java.math.BigDecimal;

/** Current account: may be emptied to 0.00 but never overdrawn. */
public class CurrentAccount extends Account {
    public CurrentAccount(String no, String owner, BigDecimal balance) { super(no, owner, balance); }
    @Override public AccountType getType() { return AccountType.CURRENT; }
    @Override public BigDecimal getMinimumBalance() { return BigDecimal.ZERO.setScale(2); }
}
