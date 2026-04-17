/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MapHud extends HudElement {
    public static final HudElementInfo<MapHud> INFO = new HudElementInfo<>(Hud.GROUP, "map", "Displays the contents of a map on your Hud.", MapHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How to determine which map to render.")
        .defaultValue(Mode.Simple)
        .build()
    );

    private final Setting<Integer> slotIndex = sgGeneral.add(new IntSetting.Builder()
        .name("slot-index")
        .description("Which slot to grab the map from.")
        .visible(() -> mode.get() == Mode.SlotIndex)
        .defaultValue(0)
        .sliderRange(0, 40)
        .build()
    );

    private final Setting<Integer> mapId = sgGeneral.add(new IntSetting.Builder()
        .name("map-id")
        .description("Which map id to render from. Must be in your inventory!")
        .visible(() -> mode.get() == Mode.MapId)
        .defaultValue(0)
        .noSlider()
        .build()
    );

    // Visual

    private final Setting<Double> scale = sgVisual.add(new DoubleSetting.Builder()
        .name("scale")
        .description("How big to render the map.")
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    private final Setting<Boolean> background = sgVisual.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgVisual.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    private final MapRenderState renderState = new MapRenderState();
    private @Nullable MapIdComponent mapComponent;
    private @Nullable MapState mapState;

    public MapHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        double scale = this.scale.get();
        this.setSize(128 * scale, 128 * scale);

        if (!Utils.canUpdate()) {
            return;
        }

        ItemStack mapStack = ItemStack.EMPTY;
        switch (mode.get()) {
            case SlotIndex -> mapStack = mc.player.getInventory().getStack(slotIndex.get());
            case MapId -> {
                FindItemResult mapResult = InvUtils.find(stack -> {
                    MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
                    return mapIdComponent != null && mapIdComponent.id() == mapId.get();
                });
                if (mapResult.found()) mapStack = mc.player.getInventory().getStack(mapResult.slot());
            }
            case Simple -> {
                FindItemResult mapResult = InvUtils.find(stack -> {
                    MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
                    return mapIdComponent != null;
                });
                if (mapResult.found()) mapStack = mc.player.getInventory().getStack(mapResult.slot());
            }
        }

        if (mapStack.isEmpty() || !mapStack.contains(DataComponentTypes.MAP_ID)) {
            mapComponent = null;
            mapState = null;
        } else {
            mapComponent = mapStack.get(DataComponentTypes.MAP_ID);
            mapState = FilledMapItem.getMapState(mapComponent, mc.world);
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mapComponent == null || mapState == null) {
            if (HudEditorScreen.isOpen()) {
                renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
                renderer.line(this.x, this.y, this.x + getWidth(), this.y + this.getHeight(), SettingColor.GRAY);
                renderer.line(this.x + getWidth(), this.y, this.x, this.y + this.getHeight(), SettingColor.GRAY);
            }

            return;
        }

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        renderer.post(() -> {
            mc.getMapRenderer().update(mapComponent, mapState, renderState);

            Matrix3x2fStack matrices = renderer.drawContext.getMatrices();
            matrices.pushMatrix();
            matrices.scale(1f / mc.getWindow().getScaleFactor());
            matrices.translate(this.x, this.y);
            matrices.scale(scale.get().floatValue());
            renderer.drawContext.drawMap(renderState);
            matrices.popMatrix();
        });
    }

    private enum Mode {
        SlotIndex,
        MapId,
        Simple
    }
}
