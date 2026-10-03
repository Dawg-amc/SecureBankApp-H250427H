package securebank;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Password hashing using PBKDF2-HMAC-SHA256 with a unique random salt per user.
 * Passwords are never stored or logged in plain text.
 */
public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Constant-time comparison to avoid timing side channels. */
    public static boolean verify(char[] password, byte[] salt, byte[] expectedHash) {
        byte[] actual = hash(password, salt);
        boolean ok = MessageDigest.isEqual(actual, expectedHash);
        Arrays.fill(actual, (byte) 0);
        return ok;
    }

    public static String enc(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    public static byte[] dec(String s) { return Base64.getDecoder().decode(s); }
}
