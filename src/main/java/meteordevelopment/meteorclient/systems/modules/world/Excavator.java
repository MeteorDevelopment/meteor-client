/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Excavator extends Module {
    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 50))
        .build()
    );
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private Status status;
    private BlockPos start;
    private BlockPos end;
    private boolean excavating;

    public Excavator() {
        super(Categories.World, "excavator", "Excavate a selection area.");
    }

    @Override
    public void onActivate() {
        status = Status.SEL_START;
        start = null;
        end = null;
        excavating = false;
    }

    @Override
    public void onDeactivate() {
        ISelection selection = baritone.getSelectionManager().getLastSelection();
        baritone.getSelectionManager().removeSelection(selection);

        if (baritone.getBuilderProcess().isActive()) baritone.getCommandManager().execute("stop");
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != GLFW_MOUSE_BUTTON_RIGHT || mc.currentScreen != null) {
            return;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult result)) return;

        if (status == Status.SEL_START) {
            start = result.getBlockPos();
            status = Status.SEL_END;
            info("Start corner set: (%d, %d, %d)".formatted(start.getX(), start.getY(), start.getZ()));
        } else if (status == Status.SEL_END) {
            end = result.getBlockPos();
            status = Status.WORKING;
            info("End corner set: (%d, %d, %d)".formatted(end.getX(), end.getY(), end.getZ()));
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (status == Status.SEL_START || status == Status.SEL_END) {
            if (!(mc.crosshairTarget instanceof BlockHitResult result)) return;
            event.renderer.box(result.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        } else if (status == Status.WORKING) {
            if (!excavating) excavate();
            if (!baritone.getBuilderProcess().isActive()) toggle();
        }
    }

    private void excavate() {
        baritone.getSelectionManager().addSelection(new BetterBlockPos(start), new BetterBlockPos(end));
        baritone.getCommandManager().execute("sel cleararea");
        excavating = true;
    }

    private enum Status {
        SEL_START,
        SEL_END,
        WORKING,
    }
}
