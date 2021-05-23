/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.profiles;

import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.accounts.Accounts;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.hud.HUD;
import minegame159.meteorclient.systems.macros.Macros;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.waypoints.Waypoints;
import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Profile implements ISerializable<Profile> {

    public String name = "";
    public boolean onLaunch = false;
    public List<String> loadOnJoinIps = new ArrayList<>();
    public boolean modules = false, config = true, hud = true, friends = false, macros = true, accounts = false, waypoints = false;

    public void load(System<?> system) {
        File folder = new File(Profiles.FOLDER, name);
        system.load(folder);
    }

    public void load() {
        File folder = new File(Profiles.FOLDER, name);

        if (modules) Modules.get().load(folder);
        if (config) Config.get().load(folder);
        if (hud) HUD.get().load(folder);
        if (friends) Friends.get().load(folder);
        if (macros) Macros.get().load(folder);
        if (accounts) Accounts.get().load(folder);
        if (waypoints) Waypoints.get().load(folder);
    }

    public void save(System<?> system) {
        File folder = new File(Profiles.FOLDER, name);
        system.save(folder);
    }

    public void save() {
        File folder = new File(Profiles.FOLDER, name);

        if (modules) Modules.get().save(folder);
        if (config) Config.get().save(folder);
        if (hud) HUD.get().save(folder);
        if (friends) Friends.get().save(folder);
        if (macros) Macros.get().save(folder);
        if (accounts) Accounts.get().save(folder);
        if (waypoints) Waypoints.get().save(folder);
    }

    public void delete(System<?> system) {
        File file = new File(new File(Profiles.FOLDER, name), system.getFile().getName());
        file.delete();
    }

    public void delete() {
        try {
            FileUtils.deleteDirectory(new File(Profiles.FOLDER, name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);

        tag.putBoolean("onLaunch", onLaunch);

        loadOnJoinIps.removeIf(String::isEmpty);

        ListTag ipsTag = new ListTag();
        for (String ip : loadOnJoinIps) ipsTag.add(StringTag.of(ip));
        tag.put("loadOnJoinIps", ipsTag);

        tag.putBoolean("modules", modules);
        tag.putBoolean("config", config);
        tag.putBoolean("hud", hud);
        tag.putBoolean("friends", friends);
        tag.putBoolean("macros", macros);
        tag.putBoolean("accounts", accounts);
        tag.putBoolean("waypoints", waypoints);

        return tag;
    }

    @Override
    public Profile fromTag(CompoundTag tag) {
        name = tag.getString("name");

        onLaunch = tag.contains("onLaunch") && tag.getBoolean("onLaunch");

        loadOnJoinIps.clear();

        if (tag.contains("loadOnJoinIps")) {
            ListTag ipsTag = tag.getList("loadOnJoinIps", 8);
            for (Tag ip : ipsTag) loadOnJoinIps.add(ip.asString());
        }

        modules = tag.getBoolean("modules") && tag.contains("modules");
        config = tag.getBoolean("config") || !tag.contains("config");
        hud = tag.getBoolean("hud") || !tag.contains("hud");
        friends = tag.getBoolean("friends") && tag.contains("friends");
        macros = tag.getBoolean("macros") || !tag.contains("macros");
        accounts = tag.getBoolean("accounts") && tag.contains("accounts");
        waypoints = tag.getBoolean("waypoints") && tag.contains("waypoints");

        return this;
    }

    public Profile set(Profile profile) {
        this.name = profile.name;

        this.onLaunch = profile.onLaunch;
        this.loadOnJoinIps = profile.loadOnJoinIps;

        this.modules = profile.modules;
        this.config = profile.config;
        this.hud = profile.hud;
        this.friends = profile.friends;
        this.macros = profile.macros;
        this.accounts = profile.accounts;
        this.waypoints = profile.waypoints;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile profile = (Profile) o;
        return name.equalsIgnoreCase(profile.name);
    }

}