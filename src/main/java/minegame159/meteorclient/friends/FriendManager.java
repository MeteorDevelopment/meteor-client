/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.friends;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.meteor.FriendListChangedEvent;
import minegame159.meteorclient.utils.entity.FriendType;
import minegame159.meteorclient.utils.files.Savable;
import minegame159.meteorclient.utils.misc.NbtUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.RainbowColorManager;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FriendManager extends Savable<FriendManager> implements Iterable<Friend> {
    public static final FriendManager INSTANCE = new FriendManager();

    private List<Friend> friends = new ArrayList<>();

    private FriendManager() {
        super(new File(MeteorClient.FOLDER, "friends.nbt"));
        RainbowColorManager.addColor(enemyColor);
        RainbowColorManager.addColor(neutralColor);
        RainbowColorManager.addColor(trustedColor);
    }

    public final SettingColor enemyColor = new SettingColor(204, 0, 0);
    public final SettingColor neutralColor = new SettingColor(0, 255, 180);
    public final SettingColor trustedColor = new SettingColor(57, 247, 47);

    public boolean attackNeutral = false;
    public boolean showEnemies = true;
    public boolean showNeutral = true;
    public boolean showTrusted = true;

    public boolean add(Friend friend) {
        if (!friends.contains(friend)) {
            friends.add(friend);
            MeteorClient.EVENT_BUS.post(FriendListChangedEvent.get());
            save();
            return true;
        }

        return false;
    }

    public List<Friend> getAll() {
        return friends;
    }

    public boolean contains(Friend friend) {
        return friends.contains(friend);
    }

    private Friend get(String name) {
        for (Friend friend : friends) {
            if (friend.name.equals(name)) {
                return friend;
            }
        }

        return null;
    }

    public Friend get(PlayerEntity player) {
        return get(player.getGameProfile().getName());
    }

    public boolean notTrusted(PlayerEntity player) {
        Friend friend = get(player);
        return friend == null || friend.type != FriendType.Trusted;
    }

    public boolean show(PlayerEntity player) {
        Friend friend = get(player);
        if (friend == null) return false;
        switch (friend.type) {
            case Enemy:
                return showEnemies;
            case Trusted:
                return showTrusted;
            default:
                return showNeutral;
        }
    }

    public boolean attack(PlayerEntity player) {
        Friend friend = get(player);
        if (friend == null) return true;
        switch (friend.type) {
            case Enemy:
                return true;
            case Trusted:
                return false;
            default:
                return attackNeutral;
        }
    }

    public Color getColor(PlayerEntity player, Color defaultColor, boolean oldAlpha) {
        Friend friend = get(player);
        if (friend == null) return defaultColor;
        Color color = null;
        switch (friend.type) {
            case Enemy:
                color = FriendManager.INSTANCE.enemyColor;
                break;
            case Trusted:
                color = FriendManager.INSTANCE.trustedColor;
                break;
            case Neutral:
                color = FriendManager.INSTANCE.neutralColor;
                break;
        }
        return new Color(color.r, color.g, color.b, oldAlpha ? defaultColor.a : 255);
    }

    public void addOrRemove(Friend friend) {
        if (friends.contains(friend)) remove(friend);
        else add(friend);
    }

    public boolean remove(Friend friend) {
        if (friends.remove(friend)) {
            MeteorClient.EVENT_BUS.post(FriendListChangedEvent.get());
            save();
            return true;
        }

        return false;
    }

    public int count() {
        return friends.size();
    }

    @Override
    public @NotNull Iterator<Friend> iterator() {
        return friends.iterator();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag friendsTag = new ListTag();

        for (Friend friend : friends) friendsTag.add(friend.toTag());
        tag.put("friends", friendsTag);
        tag.put("enemy-color", enemyColor.toTag());
        tag.put("neutral-color", neutralColor.toTag());
        tag.put("trusted-color", trustedColor.toTag());
        tag.putBoolean("attack-neutral", attackNeutral);
        tag.putBoolean("show-enemies", showEnemies);
        tag.putBoolean("show-neutral", showNeutral);
        tag.putBoolean("show-trusted", showTrusted);


        return tag;
    }

    @Override
    public FriendManager fromTag(CompoundTag tag) {
        friends = NbtUtils.listFromTag(tag.getList("friends", 10), tag1 -> new Friend((CompoundTag) tag1));
        if (tag.contains("enemy-color")) enemyColor.fromTag(tag.getCompound("enemy-color"));
        if (tag.contains("neutral-color")) neutralColor.fromTag(tag.getCompound("neutral-color"));
        if (tag.contains("trusted-color")) trustedColor.fromTag(tag.getCompound("trusted-color"));
        if (tag.contains("attack-neutral")) attackNeutral = tag.getBoolean("attack-neutral");
        if (tag.contains("show-enemies")) showEnemies = tag.getBoolean("show-enemies");
        if (tag.contains("show-neutral")) showNeutral = tag.getBoolean("show-neutral");
        if (tag.contains("show-trusted")) showTrusted = tag.getBoolean("show-trusted");
        return this;
    }
}
