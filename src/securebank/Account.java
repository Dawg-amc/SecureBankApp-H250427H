package securebank;

import java.math.BigDecimal;

/**
 * Abstract bank account (abstraction + encapsulation).
 * Balance is a BigDecimal (never float/double for money) and can only change via deposit/withdraw.
 * Subclasses define their own minimum-balance rule (polymorphism).
 */
public abstract class Account {
    private final String accountNumber;
    private final String owner;
    private BigDecimal balance;

    protected Account(String accountNumber, String owner, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = balance;
    }

    public abstract AccountType getType();
    /** The balance that must remain after any withdrawal. */
    public abstract BigDecimal getMinimumBalance();

    public String getAccountNumber() { return accountNumber; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    public void deposit(BigDecimal amount) throws BankException {
        if (amount == null || amount.signum() <= 0) throw new BankException("Deposit must be positive.");
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) throws BankException {
        if (amount == null || amount.signum() <= 0) throw new BankException("Withdrawal must be positive.");
        if (balance.subtract(amount).compareTo(getMinimumBalance()) < 0) {
            throw new BankException("Insufficient funds. Minimum balance of " + getMinimumBalance()
                    + " must be maintained.");
        }
        balance = balance.subtract(amount);
    }

    /** Used only to roll back an in-memory change if saving to disk fails. */
    void restoreBalance(BigDecimal previous) { this.balance = previous; }

    public String toRecord() {
        return String.join("|", accountNumber, owner, getType().name(), balance.toPlainString());
    }

    public static Account fromRecord(String[] f) {
        if (f.length != 4) throw new IllegalArgumentException("Bad account record");
        BigDecimal bal = new BigDecimal(f[3]);
        if (bal.signum() < 0) throw new IllegalArgumentException("Negative balance");
        return switch (AccountType.valueOf(f[2])) {
            case SAVINGS -> new SavingsAccount(f[0], f[1], bal);
            case CURRENT -> new CurrentAccount(f[0], f[1], bal);
        };
    }
}
