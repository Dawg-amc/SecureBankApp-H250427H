package securebank;

import java.math.BigDecimal;

/** Savings account: must always keep a minimum balance of 10.00. */
public class SavingsAccount extends Account {
    public SavingsAccount(String no, String owner, BigDecimal balance) { super(no, owner, balance); }
    @Override public AccountType getType() { return AccountType.SAVINGS; }
    @Override public BigDecimal getMinimumBalance() { return new BigDecimal("10.00"); }
}
