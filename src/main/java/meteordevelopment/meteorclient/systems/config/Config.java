/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.config;

import meteordevelopment.meteorclient.gui.tabs.builtin.ConfigTab;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.*;

import java.util.*;

public class Config extends System<Config> {
    public final Version version;
    public final String devBuild;

    public String font = ConfigTab.font.get();
    public boolean customFont = ConfigTab.customFont.get();
    public int rotationHoldTicks = ConfigTab.rotationHoldTicks.get();

    public String prefix = ConfigTab.prefix.get();
    public boolean openChatOnPrefix = ConfigTab.openChatOnPrefix.get();
    public boolean chatCommandsInfo = ConfigTab.chatCommandsInfo.get();
    public boolean deleteChatCommandsInfo = ConfigTab.deleteChatCommandsInfo.get();

    public boolean titleScreenCredits = ConfigTab.titleScreenCredits.get();
    public boolean titleScreenSplashes = ConfigTab.titleScreenSplashes.get();
    public boolean customWindowTitle = ConfigTab.customWindowTitle.get();
    public String customWindowTitleText = ConfigTab.customWindowTitleText.get();

    public boolean useTeamColor = ConfigTab.useTeamColor.get();

    public List<String> dontShowAgainPrompts = new ArrayList<>();

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
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("version", version.toString());

        tag.putString("font", font);
        tag.putBoolean("customFont", customFont);
        tag.putDouble("rainbowSpeed", RainbowColors.GLOBAL.getSpeed());
        tag.putInt("rotationHoldTicks", rotationHoldTicks);

        tag.putString("prefix", prefix);
        tag.putBoolean("openChatOnPrefix", openChatOnPrefix);
        tag.putBoolean("chatCommandsInfo", chatCommandsInfo);
        tag.putBoolean("deleteChatCommandsInfo", deleteChatCommandsInfo);

        tag.putBoolean("titleScreenCredits", titleScreenCredits);
        tag.putBoolean("titleScreenSplashes", titleScreenSplashes);
        tag.putBoolean("customWindowTitle", customWindowTitle);
        tag.putString("customWindowTitleText", customWindowTitleText);

        tag.putBoolean("useTeamColor", useTeamColor);

        tag.put("dontShowAgainPrompts", listToNbt(dontShowAgainPrompts));
        return tag;
    }

    @Override
    public Config fromTag(NbtCompound tag) {
        font = getString(tag, "font", ConfigTab.font);
        customFont = getBoolean(tag, "customFont", ConfigTab.customFont);
        RainbowColors.GLOBAL.setSpeed(tag.contains("rainbowSpeed") ? tag.getDouble("rainbowSpeed") : ConfigTab.rainbowSpeed.getDefaultValue() / 100);
        rotationHoldTicks = getInt(tag, "rotationHoldTicks", ConfigTab.rotationHoldTicks);

        prefix = getString(tag, "prefix", ConfigTab.prefix);
        openChatOnPrefix = getBoolean(tag, "openChatOnPrefix", ConfigTab.openChatOnPrefix);
        chatCommandsInfo = getBoolean(tag, "chatCommandsInfo", ConfigTab.chatCommandsInfo);
        deleteChatCommandsInfo = getBoolean(tag, "deleteChatCommandsInfo", ConfigTab.deleteChatCommandsInfo);

        titleScreenCredits = getBoolean(tag, "titleScreenCredits", ConfigTab.titleScreenCredits);
        titleScreenSplashes = getBoolean(tag, "titleScreenSplashes", ConfigTab.titleScreenSplashes);
        customWindowTitle = getBoolean(tag, "customWindowTitle", ConfigTab.customWindowTitle);
        customWindowTitleText = getString(tag, "customWindowTitleText", ConfigTab.customWindowTitleText);

        useTeamColor = getBoolean(tag, "useTeamColor", ConfigTab.useTeamColor);

        dontShowAgainPrompts.clear();
        for (NbtElement item: tag.getList("dontShowAgainPrompts", NbtElement.STRING_TYPE))
            dontShowAgainPrompts.add(item.asString());

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

    private NbtList listToNbt(List<String> lst) {
        NbtList nbt = new NbtList();
        for (String item: lst) 
            nbt.add(NbtString.of(item));
        return nbt;
    }
}
