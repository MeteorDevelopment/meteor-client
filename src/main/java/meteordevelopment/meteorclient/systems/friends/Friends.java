/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.friends;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Friends extends System<Friends> implements Iterable<Friend> {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color used to show friends.")
        .defaultValue(new SettingColor(0, 255, 180))
        .build()
    );

    public final Setting<Boolean> attack = sgGeneral.add(new BoolSetting.Builder()
        .name("attack")
        .description("Whether to attack friends.")
        .defaultValue(false)
        .build()
    );

    private final List<Friend> friends = new ArrayList<>();

    public Friends() {
        super("friends");
    }

    public static Friends get() {
        return Systems.get(Friends.class);
    }

    @Override
    public void init() {
        RainbowColors.add(color.get());
    }

    public boolean add(Friend friend) {
        if (friend.name.isEmpty()) return false;

        if (!friends.contains(friend)) {
            friends.add(friend);
            save();

            return true;
        }

        return false;
    }

    public boolean remove(Friend friend) {
        if (friends.remove(friend)) {
            save();
            return true;
        }

        return false;
    }

    public Friend get(String name) {
        for (Friend friend : friends) {
            if (friend.name.equals(name)) {
                return friend;
            }
        }

        return null;
    }

    public Friend get(UUID uuid) {
        for (Friend friend : friends) {
            if (friend.id.equals(uuid)) {
                return friend;
            }
        }

        return null;
    }

    public Friend get(PlayerEntity player) {
        return get(player.getUuid());
    }

    public boolean isFriend(PlayerEntity player) {
        return get(player) != null;
    }

    public boolean shouldAttack(PlayerEntity player) {
        return !isFriend(player) || attack.get();
    }

    public int count() {
        return friends.size();
    }

    @Override
    public @NotNull Iterator<Friend> iterator() {
        return friends.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        NbtList friendsTag = new NbtList();

        for (Friend friend : friends) friendsTag.add(friend.toTag());
        tag.put("friends", friendsTag);

        tag.put("settings", settings.toTag());

        return tag;
    }

    @Override
    public Friends fromTag(NbtCompound tag) {
        NbtList friendList = tag.getList("friends", 10);
        friends.clear();

        for (NbtElement friendTag : friendList) {
            NbtCompound compound = (NbtCompound) friendTag;
            if (!compound.contains("id")) continue;

            MeteorExecutor.execute(() -> {
                Friend friend = new Friend(compound);
                if (friend.updateName()) friends.add(friend);
            });
        }

        settings.fromTag(tag.getCompound("settings"));

        return this;
    }
}
