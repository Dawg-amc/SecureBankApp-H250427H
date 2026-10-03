package securebank;

/** Checked exception for all business-rule / validation failures. Messages are safe to show to users. */
public class BankException extends Exception {
    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }
}
