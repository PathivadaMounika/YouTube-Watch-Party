package com.watchparty.backend.dto;

/**
 * Returned by both create-room and join-room endpoints.
 *
 * "you" tells the caller their own userId/role - the frontend must hang
 * onto this (e.g. in sessionStorage) since it's how every future request
 * or WebSocket connection identifies who is acting.
 */
public class JoinResultResponse {

    private RoomResponse room;
    private ParticipantDto you;

    public JoinResultResponse(RoomResponse room, ParticipantDto you) {
        this.room = room;
        this.you = you;
    }

    public RoomResponse getRoom() {
        return room;
    }

    public ParticipantDto getYou() {
        return you;
    }
}
