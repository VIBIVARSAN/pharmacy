package com.pharmacy.service;

import com.pharmacy.dao.AbstractDao;
import com.pharmacy.dao.UserDao;
import com.pharmacy.exception.AuthenticationException;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.model.Role;
import com.pharmacy.model.User;
import com.pharmacy.util.AppConfig;
import com.pharmacy.util.PasswordEncoder;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.logging.Logger;

/** Login, first-administrator setup and session user lookup. */
public class AuthService extends BaseService {
    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());
    private static final String GENERIC_FAILURE = "Invalid username or password.";

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptTracker attempts = new LoginAttemptTracker();
    private final String setupToken;
    /** Compared against when the username is unknown, so both cases take similar time. */
    private final String dummyHash;

    public AuthService(TransactionManager tx, AuditService audit, UserDao userDao, PasswordEncoder passwordEncoder) {
        super(tx, audit);
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.hash("not-a-real-password");
        String configured = AppConfig.get("SETUP_TOKEN");
        if (configured != null) {
            this.setupToken = configured;
        } else {
            byte[] random = new byte[12];
            new SecureRandom().nextBytes(random);
            this.setupToken = HexFormat.of().formatHex(random);
        }
    }

    /** Prints the generated one-time setup token to the server log when no account exists yet. */
    public void announceSetupTokenIfNeeded() {
        try {
            if (isSetupRequired() && AppConfig.get("SETUP_TOKEN") == null) {
                LOG.warning("No user accounts exist. Open /setup and use this one-time setup token: " + setupToken);
            }
        } catch (RuntimeException e) {
            LOG.warning("Could not check for existing users (is the database reachable?): " + e.getMessage());
        }
    }

    public boolean isSetupRequired() {
        return tx.read(userDao::countAll) == 0;
    }

    /** Creates the very first administrator. Only possible while the users table is empty. */
    public User createFirstAdmin(String token, String username, String fullName, String password, String confirmPassword) {
        String name = clean(fullName);
        String login = clean(username);
        Validator v = new Validator();
        boolean tokenOk = token != null && MessageDigest.isEqual(token.trim().getBytes(StandardCharsets.UTF_8),
                setupToken.getBytes(StandardCharsets.UTF_8));
        v.check(tokenOk, "token", "The setup token is not correct.");
        v.username("username", login).text("fullName", name, 120, "Full name").password("password", password)
                .check(password != null && password.equals(confirmPassword), "confirmPassword", "Passwords do not match.");
        v.throwIfInvalid();
        String hash = passwordEncoder.hash(password);
        return tx.inTransaction(con -> {
            userDao.acquireSetupLock(con);
            if (userDao.countAll(con) > 0) {
                throw new BusinessRuleException("Setup has already been completed. Please sign in.");
            }
            User u = new User();
            u.setUsername(login);
            u.setFullName(name);
            u.setRole(Role.ADMIN);
            u.setActive(true);
            u.setPasswordHash(hash);
            u.setId(userDao.insert(con, u));
            audit.log(con, u, "USER_CREATED", u, "First administrator account created: " + login);
            return u;
        });
    }

    /**
     * Verifies credentials. The same generic message is used for an unknown
     * user, a wrong password and a deactivated account.
     */
    public User authenticate(String username, String password) {
        String login = username == null ? "" : username.trim();
        if (login.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Enter your username and password.");
        }
        if (login.length() > 50 || password.length() > 72) {
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        if (attempts.isLocked(login)) {
            throw new AuthenticationException("Too many failed attempts. Please wait a few minutes and try again.");
        }
        Optional<User> found = tx.read(con -> userDao.findByUsername(con, login));
        boolean passwordOk = passwordEncoder.matches(password, found.map(User::getPasswordHash).orElse(dummyHash));
        if (found.isEmpty() || !passwordOk || !found.get().isActive()) {
            attempts.recordFailure(login);
            Long userId = found.map(User::getId).orElse(null);
            audit.logStandalone(userId, "LOGIN_FAILED", "USER", userId,
                    found.isPresent() ? "Failed sign-in attempt for " + found.get().getUsername()
                            : "Failed sign-in attempt for an unknown account");
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        attempts.recordSuccess(login);
        User user = found.get();
        tx.inTransaction(con -> {
            userDao.updateLastLogin(con, user.getId(), AbstractDao.now());
            audit.log(con, user, "LOGIN", user, "Signed in");
            return null;
        });
        return user;
    }

    public void recordLogout(User user) {
        audit.logStandalone(user.getId(), "LOGOUT", "USER", user.getId(), "Signed out");
    }

    /** Re-reads the session user on every request so deactivation and role changes apply immediately. */
    public Optional<User> findActiveUser(long id) {
        return tx.read(con -> userDao.findById(con, id)).filter(User::isActive);
    }
}
