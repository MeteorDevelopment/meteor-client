/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.config;

import com.g00fy2.versioncompare.Version;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;

public class Config extends System<Config> {
    public final Version version = new Version("0.4.2");
    public String devBuild;
    private String prefix = ".";

    public boolean customFont = true;

    public boolean rainbowPrefix = false;
    public double rainbowPrefixSpeed, rainbowPrefixSpread;

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
        tag.putBoolean("rainbowPrefix", rainbowPrefix);
        tag.putDouble("rainbowPrefixSpeed", rainbowPrefixSpeed);
        tag.putDouble("rainbowPrefixSpread", rainbowPrefixSpread);
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
        customFont = !tag.contains("customFont") || tag.getBoolean("customFont");
        rainbowPrefix = tag.contains("rainbowPrefix") && tag.getBoolean("rainbowPrefix");
        rainbowPrefixSpeed = tag.getDouble("rainbowPrefixSpeed");
        rainbowPrefixSpread = tag.getDouble("rainbowPrefixSpread");
        chatCommandsInfo = !tag.contains("chatCommandsInfo") || tag.getBoolean("chatCommandsInfo");
        deleteChatCommandsInfo = !tag.contains("deleteChatCommandsInfo") || tag.getBoolean("deleteChatCommandsInfo");
        sendDataToApi = !tag.contains("sendDataToApi") || tag.getBoolean("sendDataToApi");
        titleScreenCredits = !tag.contains("titleScreenCredits") || tag.getBoolean("titleScreenCredits");
        windowTitle = tag.contains("windowTitle") && tag.getBoolean("windowTitle");

        return this;
    }
}
