/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.config;

import com.g00fy2.versioncompare.Version;
import minegame159.meteorclient.gui.tabs.builtin.ConfigTab;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.systems.System;
import minegame159.meteorclient.systems.Systems;
import minegame159.meteorclient.utils.render.color.RainbowColors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.CompoundTag;

public class Config extends System<Config> {
    public final Version version;
    public final String devBuild;

    public String font = ConfigTab.font.get();
    public boolean customFont = ConfigTab.customFont.get();
    public boolean sendDataToApi = ConfigTab.sendDataToApi.get();
    public int rotationHoldTicks = ConfigTab.rotationHoldTicks.get();

    public String prefix = ConfigTab.prefix.get();
    public boolean chatCommandsInfo = ConfigTab.chatCommandsInfo.get();
    public boolean deleteChatCommandsInfo = ConfigTab.deleteChatCommandsInfo.get();
    public boolean rainbowPrefix = ConfigTab.rainbowPrefix.get();

    public boolean titleScreenCredits = ConfigTab.titleScreenCredits.get();
    public boolean customWindowTitle = ConfigTab.customWindowTitle.get();
    public String customWindowTitleText = ConfigTab.customWindowTitleText.get();

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

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("version", version.getOriginalString());

        tag.putString("font", font);
        tag.putBoolean("customFont", customFont);
        tag.putDouble("rainbowSpeed", RainbowColors.GLOBAL.getSpeed());
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
        font = getString(tag, "font", ConfigTab.font);
        customFont = getBoolean(tag, "customFont", ConfigTab.customFont);
        RainbowColors.GLOBAL.setSpeed(tag.contains("rainbowSpeed") ? tag.getDouble("rainbowSpeed") : ConfigTab.rainbowSpeed.getDefaultValue() / 100);
        sendDataToApi = getBoolean(tag, "sendDataToApi", ConfigTab.sendDataToApi);
        rotationHoldTicks = getInt(tag, "rotationHoldTicks", ConfigTab.rotationHoldTicks);

        prefix = getString(tag, "prefix", ConfigTab.prefix);
        chatCommandsInfo = getBoolean(tag, "chatCommandsInfo", ConfigTab.chatCommandsInfo);
        deleteChatCommandsInfo = getBoolean(tag, "deleteChatCommandsInfo", ConfigTab.deleteChatCommandsInfo);
        rainbowPrefix = getBoolean(tag, "rainbowPrefix", ConfigTab.rainbowPrefix);

        titleScreenCredits = getBoolean(tag, "titleScreenCredits", ConfigTab.titleScreenCredits);
        customWindowTitle = getBoolean(tag, "customWindowTitle", ConfigTab.customWindowTitle);
        customWindowTitleText = getString(tag, "customWindowTitleText", ConfigTab.customWindowTitleText);

        return this;
    }

    private boolean getBoolean(CompoundTag tag, String key, Setting<Boolean> setting) {
        return tag.contains(key) ? tag.getBoolean(key) : setting.get();
    }

    private String getString(CompoundTag tag, String key, Setting<String> setting) {
        return tag.contains(key) ? tag.getString(key) : setting.get();
    }

    private double getDouble(CompoundTag tag, String key, Setting<Double> setting) {
        return tag.contains(key) ? tag.getDouble(key) : setting.get();
    }

    private int getInt(CompoundTag tag, String key, Setting<Integer> setting) {
        return tag.contains(key) ? tag.getInt(key) : setting.get();
    }
}
