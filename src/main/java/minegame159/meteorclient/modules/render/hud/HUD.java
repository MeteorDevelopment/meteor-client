/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.gui.screens.HudElementScreen;
import minegame159.meteorclient.gui.screens.topbar.TopBarHud;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.render.hud.modules.*;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.render.AlignmentX;
import minegame159.meteorclient.utils.render.AlignmentY;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class HUD extends Module {
    private static final HudRenderer RENDERER = new HudRenderer();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEditor = settings.createGroup("Editor");

    // General

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the HUD.")
            .defaultValue(1)
            .min(1)
            .max(3)
            .sliderMin(1)
            .sliderMax(3)
            .build()
    );

    public final Setting<SettingColor> primaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("primary-color")
            .description("Primary color of text.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    public final Setting<SettingColor> secondaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("secondary-color")
            .description("Secondary color of text.")
            .defaultValue(new SettingColor(175, 175, 175))
            .build()
    );

    // Editor

    public final Setting<Integer> snappingRange = sgEditor.add(new IntSetting.Builder()
            .name("snapping-range")
            .description("Snapping range in editor.")
            .defaultValue(6)
            .build()
    );

    public final List<HudElement> elements = new ArrayList<>();
    
    private final HudElementLayer topLeft, topCenter, topRight, bottomLeft, bottomCenter, bottomRight;

    public HUD() {
        super(Categories.Render, "HUD", "In game overlay.");

        // Top Left
        topLeft = new HudElementLayer(RENDERER, elements, AlignmentX.Left, AlignmentY.Top, 2, 2);
        topLeft.add(new WatermarkHud(this));
        topLeft.add(new FpsHud(this));
        topLeft.add(new PingHud(this));
        topLeft.add(new TpsHud(this));
        topLeft.add(new SpeedHud(this));
        topLeft.add(new BiomeHud(this));
        topLeft.add(new TimeHud(this));
        topLeft.add(new ServerHud(this));
        topLeft.add(new DurabilityHud(this));
        topLeft.add(new BreakingBlockHud(this));
        topLeft.add(new LookingAtHud(this));
        topLeft.add(new ModuleInfoHud(this));

        // Top Center
        topCenter = new HudElementLayer(RENDERER, elements, AlignmentX.Center, AlignmentY.Top, 0, 2);
        topCenter.add(new InventoryViewerHud(this));
        topCenter.add(new WelcomeHud(this));
        topCenter.add(new LagNotifierHud(this));

        // Top Right
        topRight = new HudElementLayer(RENDERER, elements, AlignmentX.Right, AlignmentY.Top, 2, 2);
        topRight.add(new ActiveModulesHud(this));
        topRight.add(new PlayersHud(this));

        // Bottom Left
        bottomLeft = new HudElementLayer(RENDERER, elements, AlignmentX.Left, AlignmentY.Bottom, 2, 2);
        bottomLeft.add(new PlayerModelHud(this));

        // Bottom Center
        bottomCenter = new HudElementLayer(RENDERER, elements, AlignmentX.Center, AlignmentY.Bottom, 48, 64);
        bottomCenter.add(new ArmorHud(this));
        bottomCenter.add(new CompassHud(this));
        bottomCenter.add(new TotemHud(this));

        // Bottom Right
        bottomRight = new HudElementLayer(RENDERER, elements, AlignmentX.Right, AlignmentY.Bottom, 2, 2);
        bottomRight.add(new PositionHud(this));
        bottomRight.add(new RotationHud(this));
        bottomRight.add(new PotionTimersHud(this));
        bottomRight.add(new CombatHud(this));

        align();
    }

    private void align() {
        RENDERER.begin(scale.get(), 0, true);

        topLeft.align();
        topCenter.align();
        topRight.align();
        bottomLeft.align();
        bottomCenter.align();
        bottomRight.align();

        RENDERER.end();
    }

    @EventHandler
    public void onRender(Render2DEvent event) {
        if (mc.options.debugEnabled) return;

        RENDERER.begin(scale.get(), event.tickDelta, false);

        for (HudElement element : elements) {
            if (element.active || mc.currentScreen instanceof TopBarHud || mc.currentScreen instanceof HudElementScreen) {
                element.update(RENDERER);
                element.render(RENDERER);
            }
        }

        RENDERER.end();
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();

        WButton reset = table.add(new WButton("Reset")).getWidget();
        reset.action = this::align;
        table.add(new WLabel("Resets positions (do this after changing scale)."));
        table.row();

        WButton editor = table.add(new WButton("Editor")).getWidget();
        editor.action = () -> mc.openScreen(new TopBarHud());
        table.add(new WLabel("Right click elements to open their settings."));

        return table;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        ListTag modulesTag = new ListTag();
        for (HudElement module : elements) modulesTag.add(module.toTag());
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        if (tag.contains("modules")) {
            ListTag modulesTag = tag.getList("modules", 10);

            for (Tag t : modulesTag) {
                CompoundTag moduleTag = (CompoundTag) t;

                HudElement module = getModule(moduleTag.getString("name"));
                if (module != null) module.fromTag(moduleTag);
            }
        }

        return super.fromTag(tag);
    }

    private HudElement getModule(String name) {
        for (HudElement module : elements) {
            if (module.name.equals(name)) return module;
        }

        return null;
    }
}
