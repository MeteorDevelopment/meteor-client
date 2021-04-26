/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.config;

import com.g00fy2.versioncompare.Version;
import minegame159.meteorclient.gui.tabs.builtin.ConfigTab;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.utils.render.color.RainbowColors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.CompoundTag;

public class Config extends System<Config> {
    public final Version version;
    public final String devBuild;

    public boolean customFont;
    public boolean sendDataToApi;
    public int rotationHoldTicks;

    private String prefix;
    public boolean chatCommandsInfo;
    public boolean deleteChatCommandsInfo;
    public boolean rainbowPrefix;

    public boolean titleScreenCredits;
    public boolean customWindowTitle;
    public String customWindowTitleText;

    public Config() {
        super("config");

        ModMetadata metadata = FabricLoader.getInstance().getModContainer("meteor-client").get().getMetadata();

        String versionString = metadata.getVersion().getFriendlyString();
        if (versionString.contains("-")) versionString = versionString.split("-")[0];

        version = new Version(versionString);
        devBuild = metadata.getCustomValue("meteor-client:devbuild").getAsString();
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

        tag.putBoolean("customFont", customFont);
        tag.putDouble("rainbowSpeed", RainbowColors.rainbowSpeed);
        tag.putBoolean("sendDataToApi", sendDataToApi);
        tag.putInt("rotationHoldTicks", rotationHoldTicks);

        tag.putString("prefix", prefix);
        tag.putBoolean("chatCommandsInfo", chatCommandsInfo);
        tag.putBoolean("deleteChatCommandsInfo", deleteChatCommandsInfo);
        tag.putBoolean("rainbowPrefix", rainbowPrefix);


        tag.putBoolean("titleScreenCredits", titleScreenCredits);
        tag.putBoolean("customWindowTitle", customWindowTitle);
        tag.putString("customWindowTitleText", customWindowTitleText);

        return tag;
    }

    @Override
    public Config fromTag(CompoundTag tag) {
        customFont = getBoolean(tag, "customFont", ConfigTab.ConfigScreen.customFont);
        RainbowColors.rainbowSpeed = tag.contains("rainbowSpeed") ? tag.getDouble("rainbowSpeed") : ConfigTab.ConfigScreen.rainbowSpeed.getDefaultValue() / 100;
        sendDataToApi = getBoolean(tag, "sendDataToApi", ConfigTab.ConfigScreen.sendDataToApi);
        rotationHoldTicks = getInt(tag, "rotationHoldTicks", ConfigTab.ConfigScreen.rotationHoldTicks);

        prefix = getString(tag, "prefix", ConfigTab.ConfigScreen.prefix);
        chatCommandsInfo = getBoolean(tag, "chatCommandsInfo", ConfigTab.ConfigScreen.chatCommandsInfo);
        deleteChatCommandsInfo = getBoolean(tag, "deleteChatCommandsInfo", ConfigTab.ConfigScreen.deleteChatCommandsInfo);
        rainbowPrefix = getBoolean(tag, "rainbowPrefix", ConfigTab.ConfigScreen.rainbowPrefix);

        titleScreenCredits = getBoolean(tag, "titleScreenCredits", ConfigTab.ConfigScreen.titleScreenCredits);
        customWindowTitle = getBoolean(tag, "customWindowTitle", ConfigTab.ConfigScreen.customWindowTitle);
        customWindowTitleText = getString(tag, "customWindowTitleText", ConfigTab.ConfigScreen.customWindowTitleText);

        return this;
    }

    private boolean getBoolean(CompoundTag tag, String key, Setting<Boolean> setting) {
        return tag.contains(key) ? tag.getBoolean(key) : setting.getDefaultValue();
    }

    private String getString(CompoundTag tag, String key, Setting<String> setting) {
        return tag.contains(key) ? tag.getString(key) : setting.getDefaultValue();
    }

    private double getDouble(CompoundTag tag, String key, Setting<Double> setting) {
        return tag.contains(key) ? tag.getDouble(key) : setting.getDefaultValue();
    }

    private int getInt(CompoundTag tag, String key, Setting<Integer> setting) {
        return tag.contains(key) ? tag.getInt(key) : setting.getDefaultValue();
    }
}
