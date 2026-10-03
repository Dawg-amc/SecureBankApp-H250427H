# Secure Banking Application

## Description
A console-based banking application written in Java that demonstrates Object-Oriented Programming
and secure coding practices. Users can register, log in, open accounts, deposit and withdraw funds,
and all data is persisted to plain text files and reloaded on start-up.

## Student Details
- **Name:** CHINGOMBE ALLAN M
- **Registration Number:** H250427H

## Features
- **User authentication:** registration and login with salted PBKDF2 password hashing
- **Account lockout:** 3 failed logins lock the user for 5 minutes (persisted across restarts)
- **Account management:** create Savings or Current accounts, view all balances
- **Transactions:** deposit and withdraw funds with business-rule checks (minimum balance, no overdraft)
- **Transaction history:** per-account statement showing time, type, amount and balance after
- **Data persistence:** users, accounts and transactions saved to text files in `data/` and loaded on start
- **Audit log:** security events (logins, failures, lockouts, transactions, denied access) in `data/audit.log`
- **Session timeout:** automatic logout after 5 minutes of inactivity

## OOP Design
| Principle | Where |
|---|---|
| Abstraction | `Account` is abstract; defines `getType()` and `getMinimumBalance()` |
| Inheritance | `SavingsAccount`, `CurrentAccount` extend `Account` |
| Polymorphism | `withdraw()` enforces each subclass's own minimum balance |
| Encapsulation | Private fields; balance changes only via `deposit()`/`withdraw()` |
| Separation of concerns | `Main` (UI), `AuthService`, `BankService` (logic), `FileRepository` (storage), `InputValidator`, `PasswordUtil` |

## Secure Coding Practices
- **Password storage:** PBKDF2-HMAC-SHA256, 120,000 iterations, unique 16-byte random salt per user; no plain-text passwords anywhere
- **Constant-time hash comparison** (`MessageDigest.isEqual`) to prevent timing attacks
- **No user enumeration:** identical error message for unknown user vs wrong password; dummy hash is computed for unknown users
- **Brute-force protection:** temporary account lockout, plus exit after repeated menu failures
- **Strong password policy:** 8-64 chars with upper, lower, digit and symbol
- **Allow-list input validation:** usernames, account numbers and amounts validated by strict regex before use
- **Injection-safe persistence:** validated fields can never contain the `|` delimiter; audit log strips CR/LF
- **Accurate money handling:** `BigDecimal` (never `double`), 2 decimal places, per-transaction cap of 1,000,000.00
- **Authorisation:** every operation verifies the account belongs to the logged-in user
- **Safe file writes:** write-to-temp then atomic move; transaction file is append-only; owner-only file permissions on POSIX
- **Failure safety:** if saving fails, the in-memory change is rolled back so memory and disk never disagree
- **Password handling:** `Console.readPassword` (no echo) and passwords wiped from memory (`char[]`) after use
- **Safe error messages:** no stack traces or internals shown to users
- **Corrupt-data tolerance:** malformed lines are skipped and logged instead of crashing the app

## Project Structure
```
SecureBankApp-[H250427H]/
├── src/securebank/   # all .java source files
├── data/             # users.txt, accounts.txt, transactions.txt, audit.log
├── run.sh / run.bat  # compile and run helpers
└── README.md
```

## How to Run
Requires **JDK 17 or later**.
```bash
./run.sh                 # Linux / macOS
run.bat                  # Windows
# or manually:
mkdir out && javac -d out src/securebank/*.java
java -cp out securebank.Main data
```
Run it from a real terminal so password input is hidden (IDE consoles show it as typed).

### Sample accounts (in `data/`)
| Username | Password |
|---|---|
| `demo` | `Demo@1234` |
| `bob` | `Bobby@1234` |


## Data File Formats
- `users.txt`: `username|salt(Base64)|hash(Base64)|failedAttempts|lockedUntilEpochMs`
- `accounts.txt`: `accountNumber|owner|TYPE|balance`
- `transactions.txt`: `ref|epochMs|accountNumber|KIND|amount|balanceAfter`

## Known Limitations
- Single-user, single-process console app (no concurrent access control)
- Data files are not encrypted (passwords are hashed, balances are plain text)
