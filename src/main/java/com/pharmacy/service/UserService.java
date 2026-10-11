package com.pharmacy.service;

import com.pharmacy.dao.UserDao;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.DataAccessException;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Permission;
import com.pharmacy.model.Role;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.PasswordEncoder;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

public class UserService extends BaseService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(TransactionManager tx, AuditService audit, UserDao userDao, PasswordEncoder passwordEncoder) {
        super(tx, audit);
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> search(User actor, PageRequest request) {
        require(actor, Permission.USER_MANAGE);
        return tx.read(con -> userDao.search(con, request));
    }

    public User get(User actor, long id) {
        require(actor, Permission.USER_MANAGE);
        return tx.read(con -> userDao.findById(con, id)).orElseThrow(() -> new NotFoundException("User"));
    }

    private Validator validateProfile(User u) {
        u.setFullName(clean(u.getFullName()));
        u.setEmail(clean(u.getEmail()));
        u.setPhone(clean(u.getPhone()));
        return new Validator().text("fullName", u.getFullName(), 120, "Full name")
                .email("email", u.getEmail()).phone("phone", u.getPhone())
                .required("role", u.getRole(), "Role");
    }

    public User create(User actor, User u, String password, String confirmPassword) {
        require(actor, Permission.USER_MANAGE);
        u.setUsername(clean(u.getUsername()));
        Validator v = validateProfile(u).username("username", u.getUsername()).password("password", password)
                .check(password != null && password.equals(confirmPassword), "confirmPassword", "Passwords do not match.");
        v.throwIfInvalid();
        u.setPasswordHash(passwordEncoder.hash(password));
        try {
            return tx.inTransaction(con -> {
                u.setId(userDao.insert(con, u));
                audit.log(con, actor, "USER_CREATED", u, "Created user " + u.getUsername() + " (" + u.getRole() + ")");
                return u;
            });
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) {
                throw new ValidationException("username", "That username is already taken.");
            }
            throw e;
        }
    }

    /** Updates name, contact details, role and active flag. Protects the last active administrator. */
    public void update(User actor, User changes) {
        require(actor, Permission.USER_MANAGE);
        validateProfile(changes).throwIfInvalid();
        tx.inTransaction(con -> {
            long admins = userDao.countActiveAdminsForUpdate(con);
            User existing = userDao.findById(con, changes.getId()).orElseThrow(() -> new NotFoundException("User"));
            boolean wasActiveAdmin = existing.isAdmin() && existing.isActive();
            boolean staysActiveAdmin = changes.getRole() == Role.ADMIN && changes.isActive();
            if (wasActiveAdmin && !staysActiveAdmin) {
                if (existing.getId().equals(actor.getId())) {
                    throw new BusinessRuleException("You cannot remove your own administrator access.");
                }
                if (admins <= 1) {
                    throw new BusinessRuleException("At least one active administrator is required.");
                }
            }
            String before = existing.getRole() + "/" + (existing.isActive() ? "active" : "inactive");
            existing.setFullName(changes.getFullName());
            existing.setEmail(changes.getEmail());
            existing.setPhone(changes.getPhone());
            existing.setRole(changes.getRole());
            existing.setActive(changes.isActive());
            userDao.update(con, existing);
            String after = existing.getRole() + "/" + (existing.isActive() ? "active" : "inactive");
            audit.log(con, actor, "USER_UPDATED", existing, "Updated user " + existing.getUsername()
                    + (before.equals(after) ? "" : " (" + before + " -> " + after + ")"));
            return null;
        });
    }

    public void resetPassword(User actor, long userId, String password, String confirmPassword) {
        require(actor, Permission.USER_MANAGE);
        new Validator().password("password", password)
                .check(password != null && password.equals(confirmPassword), "confirmPassword", "Passwords do not match.")
                .throwIfInvalid();
        String hash = passwordEncoder.hash(password);
        tx.inTransaction(con -> {
            User target = userDao.findById(con, userId).orElseThrow(() -> new NotFoundException("User"));
            userDao.updatePassword(con, userId, hash);
            audit.log(con, actor, "PASSWORD_RESET", target, "Password reset for " + target.getUsername());
            return null;
        });
    }

    /** Any signed-in user may change their own password after proving they know the current one. */
    public void changeOwnPassword(User actor, String currentPassword, String newPassword, String confirmPassword) {
        User fresh = tx.read(con -> userDao.findById(con, actor.getId())).orElseThrow(() -> new NotFoundException("User"));
        Validator v = new Validator()
                .check(passwordEncoder.matches(currentPassword, fresh.getPasswordHash()), "currentPassword",
                        "Your current password is not correct.")
                .password("newPassword", newPassword)
                .check(newPassword != null && newPassword.equals(confirmPassword), "confirmPassword", "Passwords do not match.");
        v.throwIfInvalid();
        String hash = passwordEncoder.hash(newPassword);
        tx.inTransaction(con -> {
            userDao.updatePassword(con, fresh.getId(), hash);
            audit.log(con, actor, "PASSWORD_CHANGED", fresh, "Changed own password");
            return null;
        });
    }
}
