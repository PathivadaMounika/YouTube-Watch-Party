package com.watchparty.backend.dto;

import com.watchparty.backend.model.Role;

public class AssignRoleWsRequest {

    private String userId;
    private Role role;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
