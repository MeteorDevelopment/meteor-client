/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud;

import minegame159.meteorclient.gui.tabs.builtin.HudTab;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.Settings;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.render.AnchorX;
import minegame159.meteorclient.utils.render.AnchorY;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;

public abstract class HudElement implements ISerializable<HudElement> {
    protected final HUD hud;
    protected final MinecraftClient mc;

    public String name;
    public final String title;
    public String description;

    public final BoundingBox box = new BoundingBox();

    public final Settings settings = new Settings();

    protected final SettingGroup sgBox = settings.createGroup("Box");

    public final Setting<AnchorX> xAnchor = sgBox.add(new EnumSetting.Builder<AnchorX>()
            .name("x-anchor")
            .description("Which horizontal to anchor from.")
            .defaultValue(AnchorX.Left)
            .onModuleActivated(setting -> box.boxAnchorX = setting.get())
            .onChanged(value -> box.boxAnchorX = value)
            .build()
    );

    public final Setting<AnchorY> yAnchor = sgBox.add(new EnumSetting.Builder<AnchorY>()
            .name("y-anchor")
            .description("Which vertical to anchor from.")
            .defaultValue(AnchorY.Top)
            .onModuleActivated(setting -> box.boxAnchorY = setting.get())
            .onChanged(value -> box.boxAnchorY = value)
            .build()
    );

    public HudElement(String name, String description) {
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.hud = HUD.get();
        this.mc = MinecraftClient.getInstance();
    }

    public abstract void update(HudRenderer renderer);

    public abstract void render(HudRenderer renderer);

    protected boolean isInEditor() {
        return mc.currentScreen instanceof HudTab.HudEditorScreen || mc.currentScreen instanceof HudTab.HudElementScreen || !Utils.canUpdate();
    }

    public double getAlignmentOffset(double width) {
        switch (box.boxAnchorX) {
            default:     return 0;
            case Center: return box.width / 2.0 - width / 2.0;
            case Right:  return box.width - width;
        }
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.put("settings", settings.toTag());
        tag.put("box", box.toTag());
        tag.putString("class", getClass().getName());

        return tag;
    }

    @Override
    public HudElement fromTag(CompoundTag tag) {
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));
        box.fromTag(tag.getCompound("box"));

        return this;
    }
}
