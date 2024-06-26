/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.elements.*;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Hud extends System<Hud> implements Iterable<HudElement> {
    public static final HudGroup GROUP = new HudGroup("Meteor");

    public boolean active;
    public Settings settings = new Settings();

    public final Map<String, HudElementInfo<?>> infos = new TreeMap<>();
    private final List<HudElement> elements = new ArrayList<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEditor = settings.createGroup("Editor");
    private final SettingGroup sgKeybind = settings.createGroup("Bind");

    // General

    private final Setting<Boolean> customFont = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-font")
        .description("Text will use custom font.")
        .defaultValue(true)
        .onChanged(aBoolean -> {
            for (HudElement element : elements) element.onFontChanged();
        })
        .build()
    );

    private final Setting<Boolean> hideInMenus = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-in-menus")
        .description("Hides the meteor hud when in inventory screens or game menus.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> textScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Scale of text if not overridden by the element.")
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    public final Setting<List<SettingColor>> textColors = sgGeneral.add(new ColorListSetting.Builder()
        .name("text-colors")
        .description("Colors used for the Text element.")
        .defaultValue(List.of(new SettingColor(), new SettingColor(175, 175, 175), new SettingColor(25, 225, 25), new SettingColor(225, 25, 25)))
        .build()
    );

    // Editor

    public final Setting<Integer> border = sgEditor.add(new IntSetting.Builder()
        .name("border")
        .description("Space around the edges of the screen.")
        .defaultValue(4)
        .sliderMax(20)
        .build()
    );

    public final Setting<Integer> snappingRange = sgEditor.add(new IntSetting.Builder()
        .name("snapping-range")
        .description("Snapping range in editor.")
        .defaultValue(10)
        .sliderMax(20)
        .build()
    );

    // Keybindings
    @SuppressWarnings("unused")
    private final Setting<Keybind> keybind = sgKeybind.add(new KeybindSetting.Builder()
        .name("bind")
        .defaultValue(Keybind.none())
        .action(() -> active = !active)
        .build()
    );

    private boolean resetToDefaultElements;

    public Hud() {
        super("hud");
    }

    public static Hud get() {
        return Systems.get(Hud.class);
    }

    @Override
    public void init() {
        settings.registerColorSettings(null);

        register(MeteorTextHud.INFO);
        register(ItemHud.INFO);
        register(InventoryHud.INFO);
        register(CompassHud.INFO);
        register(ArmorHud.INFO);
        register(HoleHud.INFO);
        register(PlayerModelHud.INFO);
        register(ActiveModulesHud.INFO);
        register(LagNotifierHud.INFO);
        register(PlayerRadarHud.INFO);
        register(ModuleInfosHud.INFO);
        register(PotionTimersHud.INFO);
        register(CombatHud.INFO);

        // Default config
        if (isFirstInit) resetToDefaultElements();
    }

    public void register(HudElementInfo<?> info) {
        infos.put(info.name, info);
    }

    private void add(HudElement element, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        element.box.setPos(x, y);

        if (xAnchor == null || yAnchor == null) element.box.updateAnchors();
        else {
            element.box.xAnchor = xAnchor;
            element.box.yAnchor = yAnchor;
        }

        element.settings.registerColorSettings(null);

        elements.add(element);
    }

    public void add(HudElementInfo<?> info, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        add(info.create(), x, y, xAnchor, yAnchor);
    }

    public void add(HudElementInfo<?> info, int x, int y) {
        add(info, x, y, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void add(HudElementInfo.Preset preset, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        HudElement element = preset.info.create();
        preset.callback.accept(element);
        add(element, x, y, xAnchor, yAnchor);
    }

    public void add(HudElementInfo<?>.Preset preset, int x, int y) {
        add(preset, x, y, null, null);
    }

    void remove(HudElement element) {
        element.settings.unregisterColorSettings();
        elements.remove(element);
    }

    public void clear() {
        elements.clear();
    }

    public void resetToDefaultElements() {
        resetToDefaultElements = true;
    }

    private void resetToDefaultElementsImpl() {
        elements.clear();

        int h = (int) Math.ceil(HudRenderer.INSTANCE.textHeight(true));

        // Top Left
        add(MeteorTextHud.WATERMARK, 4, 4, XAnchor.Left, YAnchor.Top);
        add(MeteorTextHud.FPS, 4, 4 + h, XAnchor.Left, YAnchor.Top);
        add(MeteorTextHud.TPS, 4, 4 + h * 2, XAnchor.Left, YAnchor.Top);
        add(MeteorTextHud.PING, 4, 4 + h * 3, XAnchor.Left, YAnchor.Top);
        add(MeteorTextHud.SPEED, 4, 4 + h * 4, XAnchor.Left, YAnchor.Top);

        // Top Right
        add(ActiveModulesHud.INFO, -4, 4, XAnchor.Right, YAnchor.Top);

        // Bottom Right
        add(MeteorTextHud.POSITION, -4, -4, XAnchor.Right, YAnchor.Bottom);
        add(MeteorTextHud.OPPOSITE_POSITION, -4, -4 - h, XAnchor.Right, YAnchor.Bottom);
        add(MeteorTextHud.ROTATION, -4, -4 - h * 2, XAnchor.Right, YAnchor.Bottom);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Utils.isLoading()) return;

        if (resetToDefaultElements) {
            resetToDefaultElementsImpl();
            resetToDefaultElements = false;
        }

        if (!(active || HudEditorScreen.isOpen())) return;

        for (HudElement element : elements) {
            if (element.isActive()) element.tick(HudRenderer.INSTANCE);
        }
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        if (Utils.isLoading()) return;

        if (!active || shouldHideHud()) return;
        if ((mc.options.hudHidden || mc.inGameHud.getDebugHud().shouldShowDebugHud()) && !HudEditorScreen.isOpen()) return;

        HudRenderer.INSTANCE.begin(event.drawContext);

        for (HudElement element : elements) {
            element.updatePos();

            if (element.isActive()) element.render(HudRenderer.INSTANCE);
        }

        HudRenderer.INSTANCE.end();
    }

    private boolean shouldHideHud() {
        return hideInMenus.get() && mc.currentScreen != null && !(mc.currentScreen instanceof WidgetScreen);
    }

    @EventHandler
    private void onCustomFontChanged(CustomFontChangedEvent event) {
        if (customFont.get()) {
            for (HudElement element : elements) element.onFontChanged();
        }
    }

    public boolean hasCustomFont() {
        return customFont.get();
    }

    public double getTextScale() {
        return textScale.get();
    }

    @NotNull
    @Override
    public Iterator<HudElement> iterator() {
        return elements.iterator();
    }

    // Serialization

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putInt("__version__", 1);

        tag.putBoolean("active", active);
        tag.put("settings", settings.toTag());
        tag.put("elements", NbtUtils.listToTag(elements));

        return tag;
    }

    @Override
    public Hud fromTag(NbtCompound tag) {
        if (!tag.contains("__version__")) {
            resetToDefaultElements();
            return this;
        }

        active = tag.getBoolean("active");
        settings.fromTag(tag.getCompound("settings"));

        // Elements
        elements.clear();

        for (NbtElement e : tag.getList("elements", NbtElement.COMPOUND_TYPE)) {
            NbtCompound c = (NbtCompound) e;
            if (!c.contains("name")) continue;

            HudElementInfo<?> info = infos.get(c.getString("name"));
            if (info != null) {
                HudElement element = info.create();
                element.fromTag(c);
                elements.add(element);
            }
        }

        return this;
    }
}
