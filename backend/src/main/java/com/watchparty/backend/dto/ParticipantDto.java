package com.watchparty.backend.dto;

import com.watchparty.backend.model.Participant;
import com.watchparty.backend.model.Role;

public class ParticipantDto {

    private String userId;
    private String username;
    private Role role;

    public ParticipantDto() {
    }

    public ParticipantDto(String userId, String username, Role role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public static ParticipantDto from(Participant participant) {
        return new ParticipantDto(
                participant.getUserId(),
                participant.getUsername(),
                participant.getRole()
        );
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
