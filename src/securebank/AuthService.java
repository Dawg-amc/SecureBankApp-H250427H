package securebank;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registration and login with salted hashing, generic error messages and temporary lockout. */
public class AuthService {
    static final int MAX_ATTEMPTS = 3;
    static final long LOCK_MILLIS = 5 * 60 * 1000L;
    private static final String GENERIC_FAIL = "Invalid username or password.";

    private final FileRepository repo;
    private final AuditLogger audit;
    private final Map<String, User> users = new LinkedHashMap<>();
    // Used so unknown usernames cost the same time as known ones (prevents timing-based user enumeration).
    private static final byte[] DUMMY_SALT = PasswordUtil.newSalt();
    private static final byte[] DUMMY_HASH = PasswordUtil.hash("dummy-Password1!".toCharArray(), DUMMY_SALT);

    public AuthService(FileRepository repo, AuditLogger audit) throws IOException {
        this.repo = repo;
        this.audit = audit;
        for (User u : repo.loadUsers(audit)) users.put(u.getUsername(), u);
    }

    public void register(String rawUsername, char[] password) throws BankException {
        String username = InputValidator.username(rawUsername);
        InputValidator.passwordStrength(password);
        if (users.containsKey(username)) throw new BankException("That username is not available.");
        byte[] salt = PasswordUtil.newSalt();
        User u = new User(username, salt, PasswordUtil.hash(password, salt), 0, 0);
        users.put(username, u);
        try {
            repo.saveUsers(users.values());
        } catch (IOException e) {
            users.remove(username);
            throw new BankException("Could not save your registration. Please try again.");
        }
        audit.log("REGISTER", username, "new user created");
    }

    public User login(String rawUsername, char[] password) throws BankException {
        String username = rawUsername == null ? "" : rawUsername.trim().toLowerCase();
        User u = users.get(username);
        long now = System.currentTimeMillis();

        if (u == null) {
            PasswordUtil.verify(password, DUMMY_SALT, DUMMY_HASH);
            audit.log("LOGIN_FAIL", username, "unknown user");
            throw new BankException(GENERIC_FAIL);
        }
        if (u.isLocked(now)) {
            audit.log("LOGIN_BLOCKED", username, "account locked");
            throw new BankException("Too many failed attempts. Account locked for a few minutes.");
        }
        if (PasswordUtil.verify(password, u.getSalt(), u.getPasswordHash())) {
            u.setFailedAttempts(0);
            u.setLockedUntil(0);
            persist();
            audit.log("LOGIN_OK", username, "-");
            return u;
        }
        u.setFailedAttempts(u.getFailedAttempts() + 1);
        if (u.getFailedAttempts() >= MAX_ATTEMPTS) {
            u.setLockedUntil(now + LOCK_MILLIS);
            u.setFailedAttempts(0);
            audit.log("ACCOUNT_LOCKED", username, "too many failed logins");
        } else {
            audit.log("LOGIN_FAIL", username, "bad password");
        }
        persist();
        throw new BankException(GENERIC_FAIL);
    }

    private void persist() {
        try { repo.saveUsers(users.values()); }
        catch (IOException e) { System.err.println("Warning: could not save login state."); }
    }
}
