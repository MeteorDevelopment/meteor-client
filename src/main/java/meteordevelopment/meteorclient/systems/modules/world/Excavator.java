/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Excavator extends Module {
    private Status status;
    private BlockPos start;
    private BlockPos end;
    private boolean excavating;

    // TODO: add some feedback to notify the player about the start/end blocks being selected.
    // This could be done either by sending a ChatUtils.sendMsg or by rendering the sel 1 & sel 2 (this option would be better)


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
        clearSelection();
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess().isActive()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("stop");
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != GLFW_MOUSE_BUTTON_RIGHT || mc.currentScreen != null) {
            return;
        }

        if (mc.crosshairTarget instanceof BlockHitResult result) {
            if (status == Status.SEL_START) {
                start = result.getBlockPos();
                status = Status.SEL_END;
//                ChatUtils.sendMsg(Text.of(String.format("Start corner set: X=%d, Y=%d, Z=%d", start.getX(), start.getY(), start.getZ())));

            } else if (status == Status.SEL_END) {
                end = result.getBlockPos();
                status = Status.WORKING;
//                ChatUtils.sendMsg(Text.of(String.format("End corner set: X=%d, Y=%d, Z=%d", end.getX(), end.getY(), end.getZ())));
            }
        }
    }

    @EventHandler
    private void onTick(Render3DEvent event) {
        switch (status) {
            case SEL_START, SEL_END -> {
                if (mc.crosshairTarget instanceof BlockHitResult result) {
                    event.renderer.box(result.getBlockPos(), new Color(255, 255, 0, 150), new Color(255, 255, 255, 150), ShapeMode.Lines, 0);
                    // TODO: replace the colors in here with customizeable ones (recycle the BlockSelection settings?)
                }
            }
            case WORKING -> {
                if (!excavating) excavate();
                if (!BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess().isActive()) {
                    clearSelection();
                    this.toggle();
                }
            }

        }
    }

    private static void clearSelection() {
        ISelection selection = BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().getLastSelection();
        BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().removeSelection(selection);
    }

    private void excavate() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().addSelection(new BetterBlockPos(start), new BetterBlockPos(end));
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("sel cleararea");
        excavating = true;
    }

    private enum Status {
        SEL_START,
        SEL_END,
        WORKING,
    }
}
