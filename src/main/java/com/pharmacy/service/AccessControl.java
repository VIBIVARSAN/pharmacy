package com.pharmacy.service;

import com.pharmacy.exception.AuthorizationException;
import com.pharmacy.model.Permission;
import com.pharmacy.model.User;

/** Server-side authorization check used by every service before it does any work. */
public final class AccessControl {
    private AccessControl() {
    }

    public static void require(User actor, Permission permission) {
        if (actor == null || !actor.can(permission)) {
            throw new AuthorizationException("You do not have permission to perform this action.");
        }
    }
}
