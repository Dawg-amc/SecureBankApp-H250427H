package securebank;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/** Console UI for the Secure Banking Application. */
public class Main {
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final int MAX_MENU_ERRORS = 5;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private final Console console = System.console();
    private final AuthService auth;
    private final BankService bank;
    private long lastActivity = System.currentTimeMillis();

    private Main(Path dataDir) throws IOException {
        FileRepository repo = new FileRepository(dataDir);
        AuditLogger audit = new AuditLogger(dataDir.resolve("audit.log"));
        this.auth = new AuthService(repo, audit);
        this.bank = new BankService(repo, audit);
    }

    public static void main(String[] args) {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        try {
            new Main(dir).run();
        } catch (IOException e) {
            System.err.println("Fatal: could not initialise data storage.");
        }
    }

    private void run() {
        System.out.println("=== Secure Banking Application ===");
        int errors = 0;
        while (true) {
            System.out.println("\n1. Login\n2. Register\n3. Exit");
            String c = prompt("Choose: ");
            if (c == null || "3".equals(c)) break;
            try {
                if ("1".equals(c)) {
                    User u = doLogin();
                    session(u);
                } else if ("2".equals(c)) {
                    doRegister();
                } else {
                    System.out.println("Invalid choice.");
                }
                errors = 0;
            } catch (BankException e) {
                System.out.println("Error: " + e.getMessage());
                if (++errors >= MAX_MENU_ERRORS) {
                    System.out.println("Too many failed attempts. Exiting.");
                    break;
                }
            }
        }
        System.out.println("Goodbye.");
    }

    private void doRegister() throws BankException {
        String u = prompt("Choose a username: ");
        char[] p1 = readPassword("Choose a password: ");
        char[] p2 = readPassword("Confirm password: ");
        try {
            if (!Arrays.equals(p1, p2)) throw new BankException("Passwords do not match.");
            auth.register(u, p1);
            System.out.println("Registration successful. You can now log in.");
        } finally {
            wipe(p1); wipe(p2);
        }
    }

    private User doLogin() throws BankException {
        String u = prompt("Username: ");
        char[] p = readPassword("Password: ");
        try {
            User user = auth.login(u, p);
            System.out.println("Welcome, " + user.getUsername() + "!");
            return user;
        } finally {
            wipe(p);
        }
    }

    private void session(User user) {
        lastActivity = System.currentTimeMillis();
        while (true) {
            System.out.println("\n--- Menu (" + user.getUsername() + ") ---");
            System.out.println("1. Create account\n2. View accounts & balances\n3. Deposit\n4. Withdraw\n5. Transaction history\n6. Logout");
            String c = prompt("Choose: ");
            if (sessionExpired()) {
                System.out.println("Session expired due to inactivity. Please log in again.");
                return;
            }
            if (c == null || "6".equals(c)) { System.out.println("Logged out."); return; }
            try {
                switch (c) {
                    case "1" -> createAccount(user);
                    case "2" -> viewAccounts(user);
                    case "3" -> transact(user, true);
                    case "4" -> transact(user, false);
                    case "5" -> history(user);
                    default -> System.out.println("Invalid choice.");
                }
            } catch (BankException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void createAccount(User user) throws BankException {
        System.out.println("Account type: 1) Savings (min balance 10.00)  2) Current");
        AccountType type = InputValidator.accountType(prompt("Choose: "));
        BigDecimal initial = InputValidator.amount(prompt("Opening deposit: "));
        Account a = bank.createAccount(user, type, initial);
        System.out.println("Account created. Number: " + a.getAccountNumber() + " | Balance: " + a.getBalance());
    }

    private void viewAccounts(User user) {
        List<Account> list = bank.getAccountsFor(user);
        if (list.isEmpty()) { System.out.println("You have no accounts yet."); return; }
        System.out.printf("%-12s %-8s %15s%n", "Account", "Type", "Balance");
        for (Account a : list) {
            System.out.printf("%-12s %-8s %15s%n", a.getAccountNumber(), a.getType(), a.getBalance());
        }
    }

    private void transact(User user, boolean deposit) throws BankException {
        String acc = InputValidator.accountNumber(prompt("Account number: "));
        BigDecimal amt = InputValidator.amount(prompt("Amount: "));
        Transaction t = deposit ? bank.deposit(user, acc, amt) : bank.withdraw(user, acc, amt);
        System.out.println((deposit ? "Deposited " : "Withdrew ") + amt + ". New balance: " + t.getBalanceAfter()
                + " (ref " + t.getId() + ")");
    }

    private void history(User user) throws BankException {
        String acc = InputValidator.accountNumber(prompt("Account number: "));
        List<Transaction> list = bank.history(user, acc);
        if (list.isEmpty()) { System.out.println("No transactions."); return; }
        System.out.printf("%-8s %-19s %-11s %12s %12s%n", "Ref", "Time", "Type", "Amount", "Balance");
        for (Transaction t : list) {
            System.out.printf("%-8s %-19s %-11s %12s %12s%n", t.getId(), FMT.format(t.getTime()),
                    t.getKind(), t.getAmount(), t.getBalanceAfter());
        }
    }

    // ---------- helpers ----------
    private boolean sessionExpired() {
        long now = System.currentTimeMillis();
        boolean expired = now - lastActivity > SESSION_TIMEOUT_MS;
        lastActivity = now;
        return expired;
    }

    private String prompt(String msg) {
        System.out.print(msg);
        try {
            String s = in.readLine();
            return s == null ? null : s.trim();
        } catch (IOException e) {
            return null;
        }
    }

    /** Uses Console.readPassword (no echo) when available; falls back to visible input in IDE consoles. */
    private char[] readPassword(String msg) {
        if (console != null) {
            char[] p = console.readPassword(msg);
            return p == null ? new char[0] : p;
        }
        String s = prompt(msg);
        return s == null ? new char[0] : s.toCharArray();
    }

    private static void wipe(char[] c) { if (c != null) Arrays.fill(c, '\0'); }
}
