package com.watchparty.backend.service;

import com.watchparty.backend.model.Role;
import org.springframework.stereotype.Service;

/**
 * Centralizes "who is allowed to do what" so permission logic doesn't get
 * duplicated (and drift out of sync) across every WebSocket handler.
 */
@Service
public class RoleService {

    public boolean canControlPlayback(Role role) {
        return role == Role.HOST || role == Role.MODERATOR;
    }

    public boolean isHost(Role role) {
        return role == Role.HOST;
    }
}
