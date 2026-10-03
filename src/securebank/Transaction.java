package securebank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable record of a deposit or withdrawal. */
public final class Transaction {
    public enum Kind { DEPOSIT, WITHDRAWAL, OPENING }

    private final String id;
    private final Instant time;
    private final String accountNumber;
    private final Kind kind;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;

    public Transaction(String id, Instant time, String accountNumber, Kind kind, BigDecimal amount, BigDecimal balanceAfter) {
        this.id = id; this.time = time; this.accountNumber = accountNumber;
        this.kind = kind; this.amount = amount; this.balanceAfter = balanceAfter;
    }

    public static Transaction create(String accountNumber, Kind kind, BigDecimal amount, BigDecimal balanceAfter) {
        return new Transaction(UUID.randomUUID().toString().substring(0, 8), Instant.now(),
                accountNumber, kind, amount, balanceAfter);
    }

    public String getId() { return id; }
    public Instant getTime() { return time; }
    public String getAccountNumber() { return accountNumber; }
    public Kind getKind() { return kind; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }

    public String toRecord() {
        return String.join("|", id, String.valueOf(time.toEpochMilli()), accountNumber, kind.name(),
                amount.toPlainString(), balanceAfter.toPlainString());
    }

    public static Transaction fromRecord(String[] f) {
        if (f.length != 6) throw new IllegalArgumentException("Bad transaction record");
        return new Transaction(f[0], Instant.ofEpochMilli(Long.parseLong(f[1])), f[2],
                Kind.valueOf(f[3]), new BigDecimal(f[4]), new BigDecimal(f[5]));
    }
}
