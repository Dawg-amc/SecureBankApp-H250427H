package securebank;

/** A registered user. Only a salted hash of the password is kept. */
public class User {
    private final String username;
    private final byte[] salt;
    private final byte[] passwordHash;
    private int failedAttempts;
    private long lockedUntil; // epoch millis; 0 = not locked

    public User(String username, byte[] salt, byte[] passwordHash, int failedAttempts, long lockedUntil) {
        this.username = username;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
    }

    public String getUsername() { return username; }
    byte[] getSalt() { return salt; }
    byte[] getPasswordHash() { return passwordHash; }
    public int getFailedAttempts() { return failedAttempts; }
    public long getLockedUntil() { return lockedUntil; }

    public boolean isLocked(long now) { return lockedUntil > now; }
    void setFailedAttempts(int n) { this.failedAttempts = n; }
    void setLockedUntil(long t) { this.lockedUntil = t; }

    public String toRecord() {
        return String.join("|", username, PasswordUtil.enc(salt), PasswordUtil.enc(passwordHash),
                String.valueOf(failedAttempts), String.valueOf(lockedUntil));
    }

    public static User fromRecord(String[] f) {
        if (f.length != 5) throw new IllegalArgumentException("Bad user record");
        return new User(f[0], PasswordUtil.dec(f[1]), PasswordUtil.dec(f[2]),
                Integer.parseInt(f[3]), Long.parseLong(f[4]));
    }
}
