/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.profiles;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Profile implements ISerializable<Profile> {

    public String name = "";
    public boolean onLaunch = false;
    public List<String> loadOnJoinIps = new ArrayList<>();
    public boolean accounts = false, config = true, friends = false, macros = true, modules = true, waypoints = false, hud = false;

    public void load(System<?> system) {
        File folder = new File(Profiles.FOLDER, name);
        system.load(folder);
    }

    public void load() {
        File folder = new File(Profiles.FOLDER, name);

        if (accounts) Accounts.get().load(folder);
        if (config) Config.get().load(folder);
        if (friends) Friends.get().load(folder);
        if (macros) Macros.get().load(folder);
        if (modules) Modules.get().load(folder);
        if (waypoints) Waypoints.get().load(folder);
        if (hud) HUD.get().load(folder);
    }

    public void save(System<?> system) {
        File folder = new File(Profiles.FOLDER, name);
        system.save(folder);
    }

    public void save() {
        File folder = new File(Profiles.FOLDER, name);

        if (accounts) Accounts.get().save(folder);
        if (config) Config.get().save(folder);
        if (friends) Friends.get().save(folder);
        if (macros) Macros.get().save(folder);
        if (modules) Modules.get().save(folder);
        if (waypoints) Waypoints.get().save(folder);
        if (hud) HUD.get().save(folder);
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
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.putBoolean("onLaunch", onLaunch);

        tag.putBoolean("accounts", accounts);
        tag.putBoolean("config", config);
        tag.putBoolean("friends", friends);
        tag.putBoolean("macros", macros);
        tag.putBoolean("modules", modules);
        tag.putBoolean("waypoints", waypoints);
        tag.putBoolean("hud", hud);

        loadOnJoinIps.removeIf(String::isEmpty);

        NbtList ipsTag = new NbtList();
        for (String ip : loadOnJoinIps) ipsTag.add(NbtString.of(ip));
        tag.put("loadOnJoinIps", ipsTag);

        return tag;
    }

    @Override
    public Profile fromTag(NbtCompound tag) {
        name = tag.getString("name");
        onLaunch = tag.contains("onLaunch") && tag.getBoolean("onLaunch");

        accounts = tag.contains("accounts") && tag.getBoolean("accounts");
        config = tag.contains("config") && tag.getBoolean("config");
        friends = tag.contains("friends") && tag.getBoolean("friends");
        macros = tag.contains("macros") && tag.getBoolean("macros");
        modules = tag.contains("modules") && tag.getBoolean("modules");
        waypoints = tag.contains("waypoints") && tag.getBoolean("waypoints");
        hud = tag.contains("hud") && tag.getBoolean("hud");

        loadOnJoinIps.clear();

        if (tag.contains("loadOnJoinIps")) {
            NbtList ipsTag = tag.getList("loadOnJoinIps", 8);
            for (NbtElement ip : ipsTag) loadOnJoinIps.add(ip.asString());
        }

        return this;
    }

    public Profile set(Profile profile) {
        this.name = profile.name;

        this.onLaunch = profile.onLaunch;
        this.loadOnJoinIps = profile.loadOnJoinIps;

        this.accounts = profile.accounts;
        this.config = profile.config;
        this.friends = profile.friends;
        this.macros = profile.macros;
        this.modules = profile.modules;
        this.waypoints = profile.waypoints;
        this.hud = profile.hud;

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
