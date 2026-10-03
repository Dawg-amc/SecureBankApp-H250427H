package securebank;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

/** Account and transaction business logic. Every operation re-checks ownership and persists immediately. */
public class BankService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final FileRepository repo;
    private final AuditLogger audit;
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public BankService(FileRepository repo, AuditLogger audit) throws IOException {
        this.repo = repo;
        this.audit = audit;
        for (Account a : repo.loadAccounts(audit)) accounts.put(a.getAccountNumber(), a);
        transactions.addAll(repo.loadTransactions(audit));
    }

    public Account createAccount(User user, AccountType type, BigDecimal initialDeposit) throws BankException {
        Account probe = type == AccountType.SAVINGS
                ? new SavingsAccount("x", user.getUsername(), BigDecimal.ZERO.setScale(2))
                : new CurrentAccount("x", user.getUsername(), BigDecimal.ZERO.setScale(2));
        if (initialDeposit.compareTo(probe.getMinimumBalance()) < 0) {
            throw new BankException("Opening deposit must be at least " + probe.getMinimumBalance() + " for this account type.");
        }
        String no = newAccountNumber();
        Account acc = type == AccountType.SAVINGS
                ? new SavingsAccount(no, user.getUsername(), initialDeposit)
                : new CurrentAccount(no, user.getUsername(), initialDeposit);
        accounts.put(no, acc);
        try {
            repo.saveAccounts(accounts.values());
            Transaction t = Transaction.create(no, Transaction.Kind.OPENING, initialDeposit, initialDeposit);
            repo.appendTransaction(t);
            transactions.add(t);
        } catch (IOException e) {
            accounts.remove(no);
            throw new BankException("Could not create the account. Please try again.");
        }
        audit.log("ACCOUNT_CREATED", user.getUsername(), "acc=" + no + " type=" + type);
        return acc;
    }

    public List<Account> getAccountsFor(User user) {
        List<Account> out = new ArrayList<>();
        for (Account a : accounts.values()) if (a.getOwner().equals(user.getUsername())) out.add(a);
        return out;
    }

    public BigDecimal getBalance(User user, String accNo) throws BankException {
        return owned(user, accNo).getBalance();
    }

    public Transaction deposit(User user, String accNo, BigDecimal amount) throws BankException {
        Account a = owned(user, accNo);
        return apply(user, a, amount, Transaction.Kind.DEPOSIT);
    }

    public Transaction withdraw(User user, String accNo, BigDecimal amount) throws BankException {
        Account a = owned(user, accNo);
        return apply(user, a, amount, Transaction.Kind.WITHDRAWAL);
    }

    public List<Transaction> history(User user, String accNo) throws BankException {
        owned(user, accNo);
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : transactions) if (t.getAccountNumber().equals(accNo)) out.add(t);
        return out;
    }

    private Transaction apply(User user, Account a, BigDecimal amount, Transaction.Kind kind) throws BankException {
        BigDecimal before = a.getBalance();
        if (kind == Transaction.Kind.DEPOSIT) a.deposit(amount); else a.withdraw(amount);
        try {
            repo.saveAccounts(accounts.values());
            Transaction t = Transaction.create(a.getAccountNumber(), kind, amount, a.getBalance());
            repo.appendTransaction(t);
            transactions.add(t);
            audit.log(kind.name(), user.getUsername(), "acc=" + a.getAccountNumber() + " amt=" + amount);
            return t;
        } catch (IOException e) {
            a.restoreBalance(before); // roll back so memory and disk never disagree
            throw new BankException("Transaction failed and was not applied. Please try again.");
        }
    }

    /** Ownership check: users can only touch their own accounts (prevents horizontal privilege escalation). */
    private Account owned(User user, String accNo) throws BankException {
        InputValidator.accountNumber(accNo);
        Account a = accounts.get(accNo);
        if (a == null || !a.getOwner().equals(user.getUsername())) {
            audit.log("ACCESS_DENIED", user.getUsername(), "acc=" + accNo);
            throw new BankException("Account not found.");
        }
        return a;
    }

    private String newAccountNumber() {
        String no;
        do {
            StringBuilder sb = new StringBuilder("10");
            for (int i = 0; i < 8; i++) sb.append(RANDOM.nextInt(10));
            no = sb.toString();
        } while (accounts.containsKey(no));
        return no;
    }
}
