/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.config;

import meteordevelopment.meteorclient.gui.tabs.builtin.ConfigTab;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.NbtCompound;

public class Config extends System<Config> {
    public String versionString;

    public String font = ConfigTab.font.get();
    public boolean customFont = ConfigTab.customFont.get();
    public int rotationHoldTicks = ConfigTab.rotationHoldTicks.get();

    public boolean chatCommandsInfo = ConfigTab.chatCommandsInfo.get();
    public boolean deleteChatCommandsInfo = ConfigTab.deleteChatCommandsInfo.get();
    public boolean rainbowPrefix = ConfigTab.rainbowPrefix.get();

    public boolean useTeamColor = ConfigTab.useTeamColor.get();

    public Config() {
        super("config");

        ModMetadata metadata = FabricLoader.getInstance().getModContainer("meteor-client").get().getMetadata();

        versionString = metadata.getVersion().getFriendlyString();
        if (versionString.contains("-")) versionString = versionString.split("-")[0];
    }

    public static Config get() {
        return Systems.get(Config.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("font", font);
        tag.putBoolean("customFont", customFont);
        tag.putDouble("rainbowSpeed", RainbowColors.GLOBAL.getSpeed());
        tag.putInt("rotationHoldTicks", rotationHoldTicks);

        tag.putBoolean("chatCommandsInfo", chatCommandsInfo);
        tag.putBoolean("deleteChatCommandsInfo", deleteChatCommandsInfo);
        tag.putBoolean("rainbowPrefix", rainbowPrefix);


        tag.putBoolean("useTeamColor", useTeamColor);

        return tag;
    }

    @Override
    public Config fromTag(NbtCompound tag) {
        font = getString(tag, "font", ConfigTab.font);
        customFont = getBoolean(tag, "customFont", ConfigTab.customFont);
        RainbowColors.GLOBAL.setSpeed(tag.contains("rainbowSpeed") ? tag.getDouble("rainbowSpeed") : ConfigTab.rainbowSpeed.getDefaultValue() / 100);
        rotationHoldTicks = getInt(tag, "rotationHoldTicks", ConfigTab.rotationHoldTicks);

        chatCommandsInfo = getBoolean(tag, "chatCommandsInfo", ConfigTab.chatCommandsInfo);
        deleteChatCommandsInfo = getBoolean(tag, "deleteChatCommandsInfo", ConfigTab.deleteChatCommandsInfo);
        rainbowPrefix = getBoolean(tag, "rainbowPrefix", ConfigTab.rainbowPrefix);

        useTeamColor = getBoolean(tag, "useTeamColor", ConfigTab.useTeamColor);

        return this;
    }

    private boolean getBoolean(NbtCompound tag, String key, Setting<Boolean> setting) {
        return tag.contains(key) ? tag.getBoolean(key) : setting.get();
    }

    private String getString(NbtCompound tag, String key, Setting<String> setting) {
        return tag.contains(key) ? tag.getString(key) : setting.get();
    }

    private double getDouble(NbtCompound tag, String key, Setting<Double> setting) {
        return tag.contains(key) ? tag.getDouble(key) : setting.get();
    }

    private int getInt(NbtCompound tag, String key, Setting<Integer> setting) {
        return tag.contains(key) ? tag.getInt(key) : setting.get();
    }
}
