/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class Excavator extends Module {
    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

    // Keybindings
    private final Setting<Keybind> selectionKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("selection-key")
        .description("Key to draw the selection.")
        .defaultValue(Keybind.fromButton(GLFW.GLFW_MOUSE_BUTTON_RIGHT))
        .build()
    );

    // Logging
    private final Setting<Boolean> logSelection = sgGeneral.add(new BoolSetting.Builder()
        .name("log-selection")
        .description("Logs the selection coordinates to the chat.")
        .defaultValue(true)
        .build()
    );

    // Rendering
    private final Setting<ShapeMode> shapeMode = sgRendering.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRendering.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRendering.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private Status status;
    private BlockPos start;
    private BlockPos end;

    public Excavator() {
        super(Categories.World, "excavator", "Excavate a selection area.");
    }

    @Override
    public void onActivate() {
        status = Status.SEL_START;
        start = null;
        end = null;
    }

    @Override
    public void onDeactivate() {
        if (status == Status.SEL_END) baritone.getCommandManager().execute("sel clear");
        else baritone.getSelectionManager().removeSelection(baritone.getSelectionManager().getLastSelection());

        if (baritone.getBuilderProcess().isActive()) baritone.getCommandManager().execute("stop");
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != selectionKey.get().getValue() || mc.currentScreen != null) {
            return;
        }
        selectCorners();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press || event.key != selectionKey.get().getValue() || mc.currentScreen != null) {
            return;
        }
        selectCorners();
    }

    /**
     * Selects the corners of the selection; Relays the selection to Baritone.
     */
    private void selectCorners() {
        if (!(mc.crosshairTarget instanceof BlockHitResult result)) return;

        if (status == Status.SEL_START) {
            start = result.getBlockPos();
            status = Status.SEL_END;
            baritone.getCommandManager().execute("sel 1 %d %d %d".formatted(start.getX(), start.getY(), start.getZ()));
            if (logSelection.get()) {
                info("Start corner set: (%d, %d, %d)".formatted(start.getX(), start.getY(), start.getZ()));
            }
        } else if (status == Status.SEL_END) {
            end = result.getBlockPos();
            status = Status.WORKING;
            baritone.getCommandManager().execute("sel 2 %d %d %d".formatted(end.getX(), end.getY(), end.getZ()));
            if (logSelection.get()) {
                info("End corner set: (%d, %d, %d)".formatted(end.getX(), end.getY(), end.getZ()));
            }
            baritone.getCommandManager().execute("sel cleararea");
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (status == Status.SEL_START || status == Status.SEL_END) {
            if (!(mc.crosshairTarget instanceof BlockHitResult result)) return;
            event.renderer.box(result.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        } else if (status == Status.WORKING && !baritone.getBuilderProcess().isActive()) toggle();
    }

    private enum Status {
        SEL_START,
        SEL_END,
        WORKING,
    }
}
