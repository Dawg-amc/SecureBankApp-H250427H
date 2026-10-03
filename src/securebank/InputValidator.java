package securebank;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Central place for input validation (allow-list approach).
 * Every piece of user input is validated here BEFORE it is used or written to a file.
 */
public final class InputValidator {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern ACCOUNT_NO = Pattern.compile("^\\d{10}$");
    private static final Pattern AMOUNT = Pattern.compile("^\\d{1,9}(\\.\\d{1,2})?$");
    public static final BigDecimal MAX_TRANSACTION = new BigDecimal("1000000.00");

    private InputValidator() {}

    public static String username(String s) throws BankException {
        if (s == null || !USERNAME.matcher(s.trim()).matches()) {
            throw new BankException("Username must be 3-20 characters: letters, digits or underscore only.");
        }
        return s.trim().toLowerCase();
    }

    public static void passwordStrength(char[] pw) throws BankException {
        if (pw == null || pw.length < 8 || pw.length > 64) {
            throw new BankException("Password must be 8-64 characters long.");
        }
        boolean up = false, low = false, dig = false, sym = false;
        for (char c : pw) {
            if (Character.isUpperCase(c)) up = true;
            else if (Character.isLowerCase(c)) low = true;
            else if (Character.isDigit(c)) dig = true;
            else sym = true;
        }
        if (!(up && low && dig && sym)) {
            throw new BankException("Password needs an uppercase letter, a lowercase letter, a digit and a symbol.");
        }
    }

    public static String accountNumber(String s) throws BankException {
        if (s == null || !ACCOUNT_NO.matcher(s.trim()).matches()) {
            throw new BankException("Account number must be exactly 10 digits.");
        }
        return s.trim();
    }

    /** Parses a money amount: positive, max 2 decimals, no exponent/sign, capped at MAX_TRANSACTION. */
    public static BigDecimal amount(String s) throws BankException {
        if (s == null || !AMOUNT.matcher(s.trim()).matches()) {
            throw new BankException("Invalid amount. Use a positive number with at most 2 decimal places (e.g. 150.75).");
        }
        BigDecimal a = new BigDecimal(s.trim()).setScale(2);
        if (a.signum() <= 0) throw new BankException("Amount must be greater than zero.");
        if (a.compareTo(MAX_TRANSACTION) > 0) {
            throw new BankException("Amount exceeds the per-transaction limit of " + MAX_TRANSACTION + ".");
        }
        return a;
    }

    public static AccountType accountType(String s) throws BankException {
        if ("1".equals(s)) return AccountType.SAVINGS;
        if ("2".equals(s)) return AccountType.CURRENT;
        throw new BankException("Invalid account type selection.");
    }
}
