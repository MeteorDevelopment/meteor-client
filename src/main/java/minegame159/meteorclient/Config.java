/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient;

import com.g00fy2.versioncompare.Version;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.utils.Utils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;

public class Config extends System<Config> {
    public final Version version = new Version("0.4.2");
    public String devBuild;
    private String prefix = ".";

    public boolean customFont = true;

    public boolean chatCommandsInfo = true;
    public boolean deleteChatCommandsInfo = true;

    public boolean sendDataToApi = true;
    public boolean titleScreenCredits = true;

    public int rotationHoldTicks = 9;

    public boolean windowTitle = false;

    public Config() {
        super("config");

        devBuild = FabricLoader.getInstance().getModContainer("meteor-client").get().getMetadata().getCustomValue("meteor-client:devbuild").getAsString();
    }

    public static Config get() {
        return Systems.get(Config.class);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        save();
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("version", version.getOriginalString());
        tag.putString("prefix", prefix);
        tag.putBoolean("customFont", customFont);
        tag.putBoolean("chatCommandsInfo", chatCommandsInfo);
        tag.putBoolean("deleteChatCommandsInfo", deleteChatCommandsInfo);
        tag.putBoolean("sendDataToApi", sendDataToApi);
        tag.putBoolean("titleScreenCredits", titleScreenCredits);
        tag.putBoolean("windowTitle", windowTitle);

        return tag;
    }

    @Override
    public Config fromTag(CompoundTag tag) {
        prefix = tag.getString("prefix");
        if (tag.contains("customFont")) customFont = tag.getBoolean("customFont");
        chatCommandsInfo = !tag.contains("chatCommandsInfo") || tag.getBoolean("chatCommandsInfo");
        deleteChatCommandsInfo = !tag.contains("deleteChatCommandsInfo") || tag.getBoolean("deleteChatCommandsInfo");
        sendDataToApi = !tag.contains("sendDataToApi") || tag.getBoolean("sendDataToApi");
        titleScreenCredits = !tag.contains("titleScreenCredits") || tag.getBoolean("titleScreenCredits");
        windowTitle = !tag.contains("windowTitle") || tag.getBoolean("windowTitle");

        // In 0.2.9 the default font was changed, detect when people load up 0.2.9 for the first time
        Version lastVer = new Version(tag.getString("version"));
        Version v029 = new Version("0.2.9");

        if (lastVer.isLowerThan(v029) && version.isAtLeast(v029)) {
            Fonts.reset();
        }

        // If you run 0.3.7 for the first time add meteor pvp to server list
        Version v037 = new Version("0.3.7");

        if (lastVer.isLowerThan(v037) && version.isAtLeast(v037)) {
            Utils.addMeteorPvpToServerList();
        }

        return this;
    }
}
