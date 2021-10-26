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
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;

public class Config extends System<Config> {
    // Version
    public Version version;
    public String devBuild;

    // Visual
    public String font = ConfigTab.font.get();
    public boolean customFont = ConfigTab.customFont.get();
    public double rainbowSpeed = ConfigTab.rainbowSpeed.get();
    public boolean titleScreenCredits = ConfigTab.titleScreenCredits.get();
    public boolean titleScreenSplashes = ConfigTab.titleScreenSplashes.get();
    public boolean customWindowTitle = ConfigTab.customWindowTitle.get();
    public String customWindowTitleText = ConfigTab.customWindowTitleText.get();

    // Chat
    public String prefix = ConfigTab.prefix.get();
    public boolean prefixOpensConsole = ConfigTab.prefixOpensConsole.get();
    public boolean chatFeedback = ConfigTab.chatFeedback.get();
    public boolean deleteChatFeedback = ConfigTab.deleteChatFeedback.get();

    // Misc
    public int rotationHoldTicks = ConfigTab.rotationHoldTicks.get();
    public boolean useTeamColor = ConfigTab.useTeamColor.get();
    public List<String> dontShowAgainPrompts = new ArrayList<>();

    public Config() {
        super("config");
    }

    public static Config get() {
        return Systems.get(Config.class);
    }

    @Override
    public void init() {
        ModMetadata metadata = FabricLoader.getInstance().getModContainer("meteor-client").get().getMetadata();

        String versionString = metadata.getVersion().getFriendlyString();
        if (versionString.contains("-")) versionString = versionString.split("-")[0];

        version = new Version(versionString);
        devBuild = metadata.getCustomValue("meteor-client:devbuild").getAsString();
    }

    // Serialisation

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("version", version.toString());

        tag.putString("font", font);
        tag.putBoolean("customFont", customFont);
        tag.putDouble("rainbowSpeed", ConfigTab.rainbowSpeed.get());

        tag.putString("prefix", prefix);
        tag.putBoolean("prefixOpensConsole", prefixOpensConsole);
        tag.putBoolean("chatFeedback", chatFeedback);
        tag.putBoolean("deleteChatFeedback", deleteChatFeedback);

        tag.putBoolean("titleScreenCredits", titleScreenCredits);
        tag.putBoolean("titleScreenSplashes", titleScreenSplashes);
        tag.putBoolean("customWindowTitle", customWindowTitle);
        tag.putString("customWindowTitleText", customWindowTitleText);

        tag.putInt("rotationHoldTicks", rotationHoldTicks);
        tag.putBoolean("useTeamColor", useTeamColor);
        tag.put("dontShowAgainPrompts", listToTag(dontShowAgainPrompts));

        return tag;
    }

    @Override
    public Config fromTag(NbtCompound tag) {
        font = getString(tag, "font", ConfigTab.font);
        customFont = getBoolean(tag, "customFont", ConfigTab.customFont);
        rainbowSpeed = getDouble(tag, "rainbowSpeed", ConfigTab.rainbowSpeed);

        prefix = getString(tag, "prefix", ConfigTab.prefix);
        prefixOpensConsole = getBoolean(tag, "prefixOpensConsole", ConfigTab.prefixOpensConsole);
        chatFeedback = getBoolean(tag, "chatFeedback", ConfigTab.chatFeedback);
        deleteChatFeedback = getBoolean(tag, "deleteChatFeedback", ConfigTab.deleteChatFeedback);

        titleScreenCredits = getBoolean(tag, "titleScreenCredits", ConfigTab.titleScreenCredits);
        titleScreenSplashes = getBoolean(tag, "titleScreenSplashes", ConfigTab.titleScreenSplashes);
        customWindowTitle = getBoolean(tag, "customWindowTitle", ConfigTab.customWindowTitle);
        customWindowTitleText = getString(tag, "customWindowTitleText", ConfigTab.customWindowTitleText);

        rotationHoldTicks = getInt(tag, "rotationHoldTicks", ConfigTab.rotationHoldTicks);
        useTeamColor = getBoolean(tag, "useTeamColor", ConfigTab.useTeamColor);
        dontShowAgainPrompts = listFromTag(tag, "dontShowAgainPrompts");

        return this;
    }

    // Utils

    private boolean getBoolean(NbtCompound tag, String key, Setting<Boolean> setting) {
        return tag.contains(key) ? tag.getBoolean(key) : setting.getDefaultValue();
    }

    private String getString(NbtCompound tag, String key, Setting<String> setting) {
        return tag.contains(key) ? tag.getString(key) : setting.getDefaultValue();
    }

    private double getDouble(NbtCompound tag, String key, Setting<Double> setting) {
        return tag.contains(key) ? tag.getDouble(key) : setting.getDefaultValue();
    }

    private int getInt(NbtCompound tag, String key, Setting<Integer> setting) {
        return tag.contains(key) ? tag.getInt(key) : setting.getDefaultValue();
    }

    private NbtList listToTag(List<String> list) {
        NbtList nbt = new NbtList();
        for (String item : list) nbt.add(NbtString.of(item));
        return nbt;
    }

    private List<String> listFromTag(NbtCompound tag, String key) {
        List<String> list = new ArrayList<>();
        for (NbtElement item : tag.getList(key, 8)) list.add(item.asString());
        return list;
    }
}
