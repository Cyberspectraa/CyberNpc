package com.cyberspectraa.cybernpc.dialogue;

import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;

/** A server-built dialogue snapshot; actions are revalidated on receipt. */
public record DialogueView(
        int entityId, String name, String role, String playerLine, String speech,
        List<Option> options, Relationship relationship
) {
    public DialogueView {
        options = List.copyOf(options);
    }

    public static void encode(DialogueView view, FriendlyByteBuf buf) {
        buf.writeVarInt(view.entityId());
        buf.writeUtf(view.name(), 96);
        buf.writeUtf(view.role(), 96);
        buf.writeUtf(view.playerLine(), 160);
        buf.writeUtf(view.speech(), 1024);
        buf.writeVarInt(view.options().size());
        for (Option choice : view.options()) {
            buf.writeUtf(choice.id(), 40);
            buf.writeUtf(choice.label(), 80);
        }
        // Data is per-player, saved on the server. Non-story NPCs send no
        // relationship snapshot. It is purely informational on the client.
        buf.writeBoolean(view.relationship() != null);
        if (view.relationship() != null) {
            buf.writeByte(view.relationship().trust());
            buf.writeByte(view.relationship().affection());
        }
    }

    public static DialogueView decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String name = buf.readUtf(96);
        String role = buf.readUtf(96);
        String playerLine = buf.readUtf(160);
        String speech = buf.readUtf(1024);
        int count = buf.readVarInt();
        if (count < 0 || count > 8) {
            throw new IllegalArgumentException("Invalid dialogue choice count");
        }
        List<Option> choices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            choices.add(new Option(buf.readUtf(40), buf.readUtf(80)));
        }
        Relationship relationship = null;
        if (buf.readBoolean()) {
            relationship = new Relationship(
                    Math.max(-10, Math.min(10, buf.readByte())),
                    Math.max(-10, Math.min(10, buf.readByte())));
        }
        return new DialogueView(id, name, role, playerLine, speech, choices, relationship);
    }

    /** Actual persisted Town character values; no extra friendship XP pool. */
    public record Relationship(int trust, int affection) {}

    public record Option(String id, String label) {}
}
