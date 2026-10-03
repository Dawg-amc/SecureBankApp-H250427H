package securebank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Text-file persistence. Records are pipe-delimited; all fields are validated before being written
 * so the delimiter can never be injected. Full rewrites use write-to-temp + atomic move so a crash
 * cannot leave a half-written (corrupt) data file.
 */
public class FileRepository {
    private final Path dir;
    private final Path usersFile, accountsFile, transactionsFile;

    public FileRepository(Path dir) throws IOException {
        this.dir = dir;
        Files.createDirectories(dir);
        usersFile = dir.resolve("users.txt");
        accountsFile = dir.resolve("accounts.txt");
        transactionsFile = dir.resolve("transactions.txt");
    }

    public Path getDir() { return dir; }

    public List<User> loadUsers(AuditLogger audit) throws IOException {
        List<User> out = new ArrayList<>();
        for (String line : readLines(usersFile)) {
            try { out.add(User.fromRecord(line.split("\\|", -1))); }
            catch (RuntimeException e) { audit.log("CORRUPT_RECORD", "-", "users.txt line skipped"); }
        }
        return out;
    }

    public List<Account> loadAccounts(AuditLogger audit) throws IOException {
        List<Account> out = new ArrayList<>();
        for (String line : readLines(accountsFile)) {
            try { out.add(Account.fromRecord(line.split("\\|", -1))); }
            catch (RuntimeException e) { audit.log("CORRUPT_RECORD", "-", "accounts.txt line skipped"); }
        }
        return out;
    }

    public List<Transaction> loadTransactions(AuditLogger audit) throws IOException {
        List<Transaction> out = new ArrayList<>();
        for (String line : readLines(transactionsFile)) {
            try { out.add(Transaction.fromRecord(line.split("\\|", -1))); }
            catch (RuntimeException e) { audit.log("CORRUPT_RECORD", "-", "transactions.txt line skipped"); }
        }
        return out;
    }

    public void saveUsers(Iterable<User> users) throws IOException {
        List<String> lines = new ArrayList<>();
        for (User u : users) lines.add(u.toRecord());
        writeAtomic(usersFile, lines);
    }

    public void saveAccounts(Iterable<Account> accounts) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Account a : accounts) lines.add(a.toRecord());
        writeAtomic(accountsFile, lines);
    }

    /** Transaction history is append-only: existing records are never rewritten. */
    public void appendTransaction(Transaction t) throws IOException {
        Files.writeString(transactionsFile, t.toRecord() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        restrict(transactionsFile);
    }

    private List<String> readLines(Path p) throws IOException {
        List<String> out = new ArrayList<>();
        if (!Files.exists(p)) return out;
        for (String l : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (!l.isBlank() && !l.startsWith("#")) out.add(l);
        }
        return out;
    }

    private void writeAtomic(Path target, List<String> lines) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        restrict(target);
    }

    /** Best effort: owner-only read/write on POSIX systems. */
    private void restrict(Path p) {
        try {
            Files.setPosixFilePermissions(p, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) { /* e.g. Windows */ }
    }
}
