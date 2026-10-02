package com.cyberspectraa.cybernpc.mail;

public enum MailState {
    PENDING(0),
    DELIVERED(1),
    READ(2),
    BOXED(3),
    IN_TRANSIT(4);

    private final int id;

    MailState(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static MailState fromId(int id) {
        for (MailState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return PENDING;
    }
}
