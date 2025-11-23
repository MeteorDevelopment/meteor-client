/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import meteordevelopment.meteorclient.systems.targeting.SavedPlayer;
import meteordevelopment.meteorclient.systems.targeting.Targeting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

// XXX: @Deprecated is spammed in order to make more squiggly lines and increase the probability of them being inspected

// Behaviour is NOT identical to old the Friends, some things might still break minorly.

@Deprecated // systems.targeting.Targeting
public class Friends implements Iterable<Friend> {
    private static Friends INSTANCE;

    private Friends() { }

    public static Friends get() {
        if (INSTANCE == null) INSTANCE = new Friends();
        return INSTANCE;
    }

    @Deprecated
    public boolean add(Friend friend) {
        return Targeting.get().addFriend(friend);
    }

    @Deprecated
    public boolean remove(Friend friend) {
        return Targeting.get().removeFriend(friend);
    }

    @Deprecated
    public Friend get(String name) {
        return (Friend) Targeting.getFriend(name);
    }

    @Deprecated
    public Friend get(PlayerEntity player) {
        return get(player.getName().getString());
    }

    @Deprecated
    public Friend get(PlayerListEntry player) {
        return get(player.getProfile().name());
    }

    @Deprecated
    public boolean isFriend(PlayerEntity player) {
        return Targeting.isFriend(player);
    }

    @Deprecated
    public boolean isFriend(PlayerListEntry player) {
        return Targeting.isFriend(player);
    }

    @Deprecated
    public boolean shouldAttack(PlayerEntity player) {
        return Targeting.shouldAttack(player);
    }

    @Deprecated
    public int count() {
        return Targeting.get().countFriends();
    }

    @Deprecated
    public boolean isEmpty() {
        return Targeting.get().friendsIsEmpty();
    }

    @Override
    public @NotNull Iterator<Friend> iterator() {
        Iterator<SavedPlayer> it = Targeting.get().getFriends().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Friend next() {
                return (Friend) it.next();
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }
}
