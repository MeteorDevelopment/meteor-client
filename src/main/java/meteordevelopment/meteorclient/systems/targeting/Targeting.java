/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.targeting;

import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;


@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public final class Targeting extends System<Targeting> {
    private final List<SavedPlayer> friends = new ArrayList<>();
    private final List<SavedPlayer> enemies = new ArrayList<>();

    public boolean prioritizePlayers;

    private static Targeting INSTANCE;

    public static Targeting get() {
        if (INSTANCE == null) INSTANCE = Systems.get(Targeting.class);
        return INSTANCE;
    }

    public Targeting() {
        // legacy name
        super("friends");
    }

    public boolean addFriend(SavedPlayer friend) {
        if (friend.name.isEmpty() || friend.name.contains(" ")) return false;

        enemies.remove(friend);

        if (!friends.contains(friend)) {
            friends.add(friend);
            save();

            return true;
        }

        return false;
    }

    public boolean removeFriend(SavedPlayer friend) {
        if (friends.remove(friend)) {
            save();
            return true;
        }

        return false;
    }

    public boolean addEnemy(SavedPlayer enemy) {
        if (enemy.name.isEmpty() || enemy.name.contains(" ")) return false;

        friends.remove(enemy);

        if (!enemies.contains(enemy)) {
            enemies.add(enemy);
            save();

            return true;
        }

        return false;
    }

    public boolean removeEnemy(SavedPlayer enemy) {
        if (enemies.remove(enemy)) {
            save();
            return true;
        }

        return false;
    }



    public int countFriends() {
        return friends.size();
    }

    public int countEnemies() {
        return enemies.size();
    }

    public boolean friendsIsEmpty() {
        return friends.isEmpty();
    }

    public boolean enemiesIsEmpty() {
        return enemies.isEmpty();
    }

    public static Relation getRelation(String playerName) {
        Targeting self = get();
        for (SavedPlayer friend : self.friends) {
            if (friend.name.equalsIgnoreCase(playerName)) {
                return Relation.FRIEND;
            }
        }
        for (SavedPlayer enemy : self.enemies) {
            if (enemy.name.equalsIgnoreCase(playerName)) {
                return Relation.ENEMY;
            }
        }
        return Relation.NEUTRAL;
    }

    public static boolean matchesSelector(Selector selector, Relation relation) {
        if (relation == Relation.IGNORE) return false;
        if (selector == Selector.All) return true;
        if (selector == Selector.None) return false;

        if (relation == Relation.FRIEND) return selector == Selector.Friends || selector == Selector.Nonenemies;
        if (relation == Relation.ENEMY) return selector == Selector.Enemies || selector == Selector.Nonfriends;

        return selector == Selector.Neutrals || selector == Selector.Nonfriends || selector == Selector.Nonenemies;
    }

    public static boolean matchesSelector(Selector selector, String playerName) {
        return matchesSelector(selector, getRelation(playerName));
    }

    public static boolean matchesSelector(Selector selector, PlayerListEntry player) {
        return matchesSelector(selector, getRelation(player));
    }

    public static boolean matchesSelector(Selector selector, PlayerEntity player) {
        return matchesSelector(selector, getRelation(player));
    }

    public static Relation getRelation(PlayerListEntry player) {
        return getRelation(player.getProfile().name());
    }

    public static Relation getRelation(PlayerEntity player) {
        if (player == null) return Relation.IGNORE;
        if (player == mc.player) return Relation.IGNORE;
        return getRelation(player.getName().getString());
    }

    public static SavedPlayer getFriend(String name) {
        for (SavedPlayer friend : get().friends) {
            if (friend.name.equalsIgnoreCase(name)) {
                return friend;
            }
        }

        return null;
    }

    public static SavedPlayer getFriend(PlayerEntity player) {
        return getFriend(player.getName().getString());
    }

    public static SavedPlayer getFriend(PlayerListEntry player) {
        return getFriend(player.getProfile().name());
    }

    public static SavedPlayer getEnemy(String name) {
        for (SavedPlayer enemy : get().enemies) {
            if (enemy.name.equalsIgnoreCase(name)) {
                return enemy;
            }
        }

        return null;
    }

    public static SavedPlayer getEnemy(PlayerEntity player) {
        return getEnemy(player.getName().getString());
    }

    public static SavedPlayer getEnemy(PlayerListEntry player) {
        return getEnemy(player.getProfile().name());
    }

    public static boolean isFriend(PlayerEntity player) {
        return player != null && getFriend(player) != null;
    }

    public static boolean isFriend(PlayerListEntry player) {
        return getFriend(player) != null;
    }

    public static boolean isEnemy(PlayerEntity player) {
        return player != null && getEnemy(player) != null;
    }

    public static boolean isEnemy(PlayerListEntry player) {
        return getEnemy(player) != null;
    }

    public static boolean isNeutral(PlayerEntity player) {
        return !isEnemy(player) && !isFriend(player);
    }

    public static boolean isNeutral(PlayerListEntry player) {
        return !isEnemy(player) && !isFriend(player);
    }

    public static boolean shouldAttack(PlayerEntity player) {
        return !isFriend(player);
    }


    public static void sortTargets(List<Entity> targets, @Nullable Boolean prioritizePlayers, Comparator<Entity> secondarySort) {
        boolean pp = (prioritizePlayers != null) ? prioritizePlayers : get().prioritizePlayers;

        targets.sort((a, b) -> {
            if (pp) {
                if (a instanceof PlayerEntity && !(b instanceof PlayerEntity)) return +1;
                if (!(a instanceof PlayerEntity) && b instanceof PlayerEntity) return -1;
            }

            if (a instanceof PlayerEntity pa && b instanceof PlayerEntity pb) {
                if (isEnemy(pa) && !isEnemy(pb)) return 1;
                if (!isEnemy(pa) && isEnemy(pb)) return -1;
            }
            return secondarySort.compare(a, b);
        });
    }

    public static void sortTargets(List<Entity> targets, Comparator<Entity> secondarySort) {
         sortTargets(targets, null, secondarySort);
    }

    public static void findTargets(List<Entity> output, @Nullable Predicate<Entity> matching, @Nullable Boolean prioritizePlayers, Comparator<Entity> secondarySort, int maxCount) {
        output.clear();

        if (!Utils.canUpdate()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity != null && (matching == null || matching.test(entity))) output.add(entity);
        }

        FakePlayerManager.forEach(fp -> {
            if (fp != null && (matching == null || matching.test(fp))) output.add(fp);
        });

        sortTargets(output, prioritizePlayers, secondarySort);

        // fast list trimming
        if (maxCount > 0 && output.size() > maxCount) {
            output.subList(maxCount, output.size()).clear();
        }
    }

    public static void findTargets(List<Entity> output, @Nullable Predicate<Entity> matching, Comparator<Entity> secondarySort, int maxCount) {
        findTargets(output, matching, null, secondarySort, maxCount);
    }

    public static void findTargets(List<Entity> output, @Nullable Predicate<Entity> matching, Comparator<Entity> secondarySort) {
        findTargets(output, matching, secondarySort, 0);
    }

    public static void findTargets(List<Entity> output, @Nullable Predicate<Entity> matching, @Nullable Boolean prioritizePlayers, Comparator<Entity> secondarySort) {
        findTargets(output, matching, prioritizePlayers, secondarySort, 0);
    }

    public static void findTargets(List<Entity> output, @Nullable Predicate<Entity> matching, @Nullable Boolean prioritizePlayers) {
        findTargets(output, matching, prioritizePlayers, (a, b) -> 0, 0);
    }

    public static void findTargets(List<Entity> output, @Nullable Predicate<Entity> matching) {
        findTargets(output, matching, null, (a, b) -> 0, 0);
    }

    public static void findPlayerTargets(List<PlayerEntity> output, double range, @Nullable Predicate<PlayerEntity> matching, Comparator<Entity> secondarySort, int maxCount) {
        output.clear();

        if (!Utils.canUpdate()) return;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity != null && (matching == null || matching.test(entity))) output.add(entity);
        }

        FakePlayerManager.forEach(fp -> {
            if (fp != null && (matching == null || matching.test(fp))) output.add(fp);
        });

        output.sort((a, b) -> {
            if (isEnemy(a) && !isEnemy(b)) return 1;
            if (!isEnemy(a) && isEnemy(b)) return -1;
            return secondarySort.compare(a, b);
        });

        // fast list trimming
        if (maxCount > 0 && output.size() > maxCount) {
            output.subList(maxCount, output.size()).clear();
        }
    }

    public static void findPlayerTargets(List<PlayerEntity> output, double range, @Nullable Predicate<PlayerEntity> matching, Comparator<Entity> secondarySort) {
        findPlayerTargets(output, range, matching, secondarySort, 0);
    }

    public static void findPlayerTargets(List<PlayerEntity> output, double range, @Nullable Predicate<PlayerEntity> matching) {
        findPlayerTargets(output, range, matching, (a, b) -> 0, 0);
    }

    public static void findPlayerTargets(List<PlayerEntity> output, double range, @Nullable Predicate<PlayerEntity> matching, int maxCount) {
        findPlayerTargets(output, range, matching, (a, b) -> 0, maxCount);
    }

    private static final List<Entity> ENTITIES = new ArrayList<>();

    @Nullable
    public static Entity findTarget(Comparator<Entity> secondarySort, @Nullable Predicate<Entity> matching) {
        ENTITIES.clear();
        findTargets(ENTITIES, matching, secondarySort, 1);
        if (!ENTITIES.isEmpty()) {
            return ENTITIES.getFirst();
        }

        return null;
    }

    @Nullable
    public static Entity findTarget(Comparator<Entity> secondarySort) {
        return findTarget(secondarySort, null);
    }

    @Nullable
    public static PlayerEntity findPlayerTarget(double range, Comparator<Entity> secondarySort) {
        return (PlayerEntity) findTarget(secondarySort, entity -> {
            if (!(entity instanceof PlayerEntity player)) return false;
            return isValidTarget(player, range);
        });
    }

    public static boolean isValidPlayerTarget(@Nullable PlayerEntity player) {
        if (player == null) return false;
        if (player == mc.player || player == mc.getCameraEntity()) return false;
        if (player.isDead() || player.getHealth() <= 0) return false;
        if (!shouldAttack(player)) return false;
        return EntityUtils.getGameMode(player) == GameMode.SURVIVAL || player instanceof FakePlayerEntity;
    }

    public static boolean isValidPlayerTarget(@Nullable PlayerEntity player, double range) {
        if (player == null) return false;
        return PlayerUtils.isWithin(player, range) && isValidPlayerTarget(player);
    }

    public static boolean isValidTarget(@Nullable Entity entity) {
        if (entity == null) return false;
        if (entity == mc.player || entity == mc.getCameraEntity()) return false;
        if ((entity instanceof LivingEntity le && le.isDead()) || !entity.isAlive()) return false;
        return (!(entity instanceof PlayerEntity pe)) || isValidPlayerTarget(pe);
    }

    public static boolean isValidTarget(@Nullable Entity entity, double range) {
        if (entity == null) return false;
        return PlayerUtils.isWithin(entity, range) && isValidTarget(entity);
    }


    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("friends", NbtUtils.listToTag(friends));
        tag.put("enemies", NbtUtils.listToTag(enemies));

        return tag;
    }

    private void _fromTag(List<SavedPlayer> to, NbtList from) {
        to.clear();

        for (NbtElement itemTag : from) {
            NbtCompound playerTag = (NbtCompound) itemTag;
            if (!playerTag.contains("name")) continue;

            String name = playerTag.getString("name", "");
            if (getFriend(name) != null) continue;

            String uuid = playerTag.getString("id", "");
            SavedPlayer player = !uuid.isBlank()
                ? new SavedPlayer(name, UndashedUuid.fromStringLenient(uuid))
                : new SavedPlayer(name);

            to.add(player);
        }

        Collections.sort(to);

        MeteorExecutor.execute(() -> to.forEach(SavedPlayer::updateInfo));
    }

    @Unmodifiable
    public Iterable<SavedPlayer> getFriends() {
        return Collections.unmodifiableList(friends);
    }

    @Unmodifiable
    public Iterable<SavedPlayer> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    @Override
    public Targeting fromTag(NbtCompound tag) {

        _fromTag(this.friends, tag.getListOrEmpty("friends"));
        _fromTag(this.enemies, tag.getListOrEmpty("enemies"));

        return this;
    }

    public enum Relation {
        FRIEND,
        NEUTRAL,
        IGNORE,
        ENEMY;

        @Override
        public String toString() {
            return switch (this) {
                case FRIEND -> "Friend";
                case NEUTRAL -> "Neutral";
                case ENEMY -> "Enemy";
                case IGNORE -> "Self";
            };
        }
    }

    public enum Selector {
        None,
        Enemies,
        Nonfriends,
        Neutrals,
        Nonenemies,
        Friends,
        All
    }
}
