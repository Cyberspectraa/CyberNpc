package com.cyberspectraa.cybernpc.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class NpcSocialMemory {
    static final int MAX_RELATIONSHIPS = 48;
    static final int MAX_PLAYER_REPUTATIONS = 32;

    private static final int FRIENDSHIP_THRESHOLD = 28;
    private static final int FRIEND_TRUST_THRESHOLD = 22;
    private static final int FRIEND_RIVALRY_LIMIT = 12;

    private final Map<UUID, Relationship> relationships = new LinkedHashMap<>();
    private final Map<UUID, Integer> playerReputation = new LinkedHashMap<>();

    @Nullable
    private UUID partyId;

    @Nullable
    private UUID partyLeaderId;

    void adjustRelationship(
            UUID target,
            int friendship,
            int trust,
            int respect,
            int fear,
            int rivalry
    ) {
        if (target == null) {
            return;
        }

        Relationship relation = relationships.get(target);
        if (relation == null) {
            makeRelationshipSpace();
            relation = new Relationship();
            relationships.put(target, relation);
        }

        relation.friendship = clampTrait(relation.friendship + friendship);
        relation.trust = clampTrait(relation.trust + trust);
        relation.respect = clampTrait(relation.respect + respect);
        relation.fear = clampTrait(relation.fear + fear);
        relation.rivalry = clampTrait(relation.rivalry + rivalry);

        if (relation.friend) {
            // Friendship is a persistent social milestone. Friends can still
            // become annoyed or afraid, but they do not silently stop being
            // friends because an unrelated value changed later.
            relation.friendship = Math.max(
                    relation.friendship,
                    FRIENDSHIP_THRESHOLD
            );
            relation.trust = Math.max(
                    relation.trust,
                    FRIEND_TRUST_THRESHOLD
            );
        }
    }

    boolean canBecomeFriends(UUID target) {
        Relationship relationship = relationships.get(target);
        return relationship != null
                && !relationship.friend
                && relationship.friendship >= FRIENDSHIP_THRESHOLD
                && relationship.trust >= FRIEND_TRUST_THRESHOLD
                && relationship.rivalry <= FRIEND_RIVALRY_LIMIT;
    }

    boolean markFriends(UUID target) {
        if (target == null) {
            return false;
        }

        Relationship relationship = relationships.get(target);
        if (relationship == null) {
            makeRelationshipSpace();
            relationship = new Relationship();
            relationships.put(target, relationship);
        }

        boolean newlyFriends = !relationship.friend;
        relationship.friend = true;
        relationship.friendship = Math.max(
                relationship.friendship,
                FRIENDSHIP_THRESHOLD
        );
        relationship.trust = Math.max(
                relationship.trust,
                FRIEND_TRUST_THRESHOLD
        );
        relationship.rivalry = Math.min(
                relationship.rivalry,
                FRIEND_RIVALRY_LIMIT
        );
        return newlyFriends;
    }

    void seedFriendship(
            UUID target,
            int friendship,
            int trust,
            int respect
    ) {
        adjustRelationship(
                target,
                Math.max(friendship, FRIENDSHIP_THRESHOLD),
                Math.max(trust, FRIEND_TRUST_THRESHOLD),
                Math.max(0, respect),
                0,
                0
        );
        markFriends(target);
    }

    boolean isFriend(UUID target) {
        Relationship relationship = relationships.get(target);
        return relationship != null && relationship.friend;
    }

    RelationshipSnapshot relationship(UUID target) {
        Relationship relation = relationships.get(target);
        if (relation == null) {
            return new RelationshipSnapshot(0, 0, 0, 0, 0, false);
        }

        return relation.snapshot();
    }

    int supportScore(UUID target) {
        RelationshipSnapshot relation = relationship(target);

        double score =
                relation.friendship() * 0.35D
                        + relation.trust() * 0.35D
                        + relation.respect() * 0.25D
                        - relation.fear() * 0.15D
                        - relation.rivalry() * 0.35D
                        + (relation.friend() ? 12.0D : 0.0D);

        return Mth.clamp((int) Math.round(score), -100, 100);
    }

    void adjustPlayerReputation(UUID playerId, int amount) {
        if (playerId == null || amount == 0) {
            return;
        }

        if (!playerReputation.containsKey(playerId)) {
            makePlayerReputationSpace();
        }

        playerReputation.put(
                playerId,
                Mth.clamp(playerReputation.getOrDefault(playerId, 0) + amount, -100, 100)
        );
    }

    int getPlayerReputation(UUID playerId) {
        return playerReputation.getOrDefault(playerId, 0);
    }

    List<RelationshipEntry> relationshipEntries() {
        List<RelationshipEntry> result = new ArrayList<>();
        for (Map.Entry<UUID, Relationship> entry : relationships.entrySet()) {
            result.add(new RelationshipEntry(entry.getKey(), entry.getValue().snapshot()));
        }

        result.sort(
                Comparator.comparingInt(
                        (RelationshipEntry entry) -> relationshipImportance(entry.relationship())
                ).reversed()
        );
        return result;
    }

    List<PlayerReputationEntry> playerReputationEntries() {
        List<PlayerReputationEntry> result = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : playerReputation.entrySet()) {
            result.add(new PlayerReputationEntry(entry.getKey(), entry.getValue()));
        }

        result.sort(
                Comparator.comparingInt(
                        (PlayerReputationEntry entry) -> Math.abs(entry.reputation())
                ).reversed()
        );
        return result;
    }

    boolean hasParty() {
        return partyId != null;
    }

    @Nullable
    UUID partyId() {
        return partyId;
    }

    @Nullable
    UUID partyLeaderId() {
        return partyLeaderId;
    }

    boolean isPartyLeader(UUID ownerId) {
        return partyLeaderId != null && partyLeaderId.equals(ownerId);
    }

    void setParty(UUID partyId, UUID leaderId) {
        this.partyId = partyId;
        this.partyLeaderId = leaderId;
    }

    void clearParty() {
        partyId = null;
        partyLeaderId = null;
    }

    void saveTo(CompoundTag tag) {
        ListTag relationshipList = new ListTag();
        for (Map.Entry<UUID, Relationship> mapEntry : relationships.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Target", mapEntry.getKey());

            Relationship relationship = mapEntry.getValue();
            entry.putInt("Friendship", relationship.friendship);
            entry.putInt("Trust", relationship.trust);
            entry.putInt("Respect", relationship.respect);
            entry.putInt("Fear", relationship.fear);
            entry.putInt("Rivalry", relationship.rivalry);
            entry.putBoolean("Friend", relationship.friend);
            relationshipList.add(entry);
        }
        tag.put("CyberNpcRelationships", relationshipList);

        ListTag reputationList = new ListTag();
        for (Map.Entry<UUID, Integer> mapEntry : playerReputation.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", mapEntry.getKey());
            entry.putInt("Reputation", mapEntry.getValue());
            reputationList.add(entry);
        }
        tag.put("CyberNpcPlayerReputation", reputationList);

        if (partyId != null) {
            tag.putUUID("CyberNpcPartyId", partyId);
        }

        if (partyLeaderId != null) {
            tag.putUUID("CyberNpcPartyLeader", partyLeaderId);
        }
    }

    void loadFrom(CompoundTag tag) {
        relationships.clear();
        playerReputation.clear();
        clearParty();

        ListTag relationshipList = tag.getList(
                "CyberNpcRelationships",
                Tag.TAG_COMPOUND
        );

        for (int i = 0; i < relationshipList.size() && relationships.size() < MAX_RELATIONSHIPS; i++) {
            CompoundTag entry = relationshipList.getCompound(i);
            if (!entry.hasUUID("Target")) {
                continue;
            }

            Relationship relationship = new Relationship();
            relationship.friendship = clampTrait(entry.getInt("Friendship"));
            relationship.trust = clampTrait(entry.getInt("Trust"));
            relationship.respect = clampTrait(entry.getInt("Respect"));
            relationship.fear = clampTrait(entry.getInt("Fear"));
            relationship.rivalry = clampTrait(entry.getInt("Rivalry"));
            relationship.friend = entry.getBoolean("Friend");

            if (relationship.friend) {
                relationship.friendship = Math.max(
                        relationship.friendship,
                        FRIENDSHIP_THRESHOLD
                );
                relationship.trust = Math.max(
                        relationship.trust,
                        FRIEND_TRUST_THRESHOLD
                );
            }

            relationships.put(entry.getUUID("Target"), relationship);
        }

        ListTag reputationList = tag.getList(
                "CyberNpcPlayerReputation",
                Tag.TAG_COMPOUND
        );

        for (int i = 0; i < reputationList.size() && playerReputation.size() < MAX_PLAYER_REPUTATIONS; i++) {
            CompoundTag entry = reputationList.getCompound(i);
            if (!entry.hasUUID("Player")) {
                continue;
            }

            playerReputation.put(
                    entry.getUUID("Player"),
                    Mth.clamp(entry.getInt("Reputation"), -100, 100)
            );
        }

        if (tag.hasUUID("CyberNpcPartyId")) {
            partyId = tag.getUUID("CyberNpcPartyId");
        }

        if (tag.hasUUID("CyberNpcPartyLeader")) {
            partyLeaderId = tag.getUUID("CyberNpcPartyLeader");
        }

        if (partyId == null || partyLeaderId == null) {
            clearParty();
        }
    }

    private void makeRelationshipSpace() {
        if (relationships.size() < MAX_RELATIONSHIPS) {
            return;
        }

        UUID weakest = relationships.entrySet().stream()
                .min(Comparator.comparingInt(
                        entry -> relationshipImportance(entry.getValue().snapshot())
                ))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (weakest != null) {
            relationships.remove(weakest);
        }
    }

    private void makePlayerReputationSpace() {
        if (playerReputation.size() < MAX_PLAYER_REPUTATIONS) {
            return;
        }

        UUID weakest = playerReputation.entrySet().stream()
                .min(Comparator.comparingInt(entry -> Math.abs(entry.getValue())))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (weakest != null) {
            playerReputation.remove(weakest);
        }
    }

    private static int relationshipImportance(RelationshipSnapshot relationship) {
        return relationship.friendship()
                + relationship.trust()
                + relationship.respect()
                + relationship.fear()
                + relationship.rivalry()
                + (relationship.friend() ? 100 : 0);
    }

    private static int clampTrait(int value) {
        return Mth.clamp(value, 0, 100);
    }

    private static final class Relationship {
        private int friendship;
        private int trust;
        private int respect;
        private int fear;
        private int rivalry;
        private boolean friend;

        private RelationshipSnapshot snapshot() {
            return new RelationshipSnapshot(
                    friendship,
                    trust,
                    respect,
                    fear,
                    rivalry,
                    friend
            );
        }
    }

    record RelationshipSnapshot(
            int friendship,
            int trust,
            int respect,
            int fear,
            int rivalry,
            boolean friend
    ) {
    }

    record RelationshipEntry(UUID targetId, RelationshipSnapshot relationship) {
    }

    record PlayerReputationEntry(UUID playerId, int reputation) {
    }
}
