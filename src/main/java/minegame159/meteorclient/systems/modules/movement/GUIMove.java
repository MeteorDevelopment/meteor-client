/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.mixin.CreativeInventoryScreenAccessor;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.Freecam;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.input.Input;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.item.ItemGroup;
import org.lwjgl.glfw.GLFW;

public class GUIMove extends Module {
    public enum Screens {
        GUI,
        Inventory,
        Both
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Screens> screens = sgGeneral.add(new EnumSetting.Builder<Screens>()
            .name("gUIs")
            .description("Which GUIs to move in.")
            .defaultValue(Screens.Inventory)
            .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
            .name("sneak")
            .description("Allows you to sneak while in GUIs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
            .name("jump")
            .description("Allows you to jump while in GUIs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
            .name("sprint")
            .description("Allows you to sprint while in GUIs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> arrowsRotate = sgGeneral.add(new BoolSetting.Builder()
            .name("arrows-rotate")
            .description("Allows you to use your arrow keys to rotate while in GUIs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotate-speed")
            .description("Rotation speed while in GUIs.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    public GUIMove() {
        super(Categories.Movement, "gui-move", "Allows you to perform various actions while in GUIs.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!skip()) {
            switch (screens.get()) {
                case GUI:
                    if (mc.currentScreen instanceof WidgetScreen) tickSneakJumpAndSprint();
                    break;
                case Inventory:
                    if (!(mc.currentScreen instanceof WidgetScreen)) tickSneakJumpAndSprint();
                    break;
                case Both:
                    tickSneakJumpAndSprint();
                    break;
            }
        }
    }

    public void tick() {
        if (!isActive() || skip()) return;

        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;

        if (Input.isPressed(mc.options.keyForward)) {
            mc.player.input.pressingForward = true;
            mc.player.input.movementForward++;
        } else mc.player.input.pressingForward = false;

        if (Input.isPressed(mc.options.keyBack)) {
            mc.player.input.pressingBack = true;
            mc.player.input.movementForward--;
        } else mc.player.input.pressingBack = false;

        if (Input.isPressed(mc.options.keyRight)) {
            mc.player.input.pressingRight = true;
            mc.player.input.movementSideways--;
        } else mc.player.input.pressingRight = false;

        if (Input.isPressed(mc.options.keyLeft)) {
            mc.player.input.pressingLeft = true;
            mc.player.input.movementSideways++;
        } else mc.player.input.pressingLeft = false;

        tickSneakJumpAndSprint();

        if (arrowsRotate.get()) {
            for (int i = 0; i < (rotateSpeed.get() * 2); i++) {
                if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) mc.player.yaw -= 0.5;
                if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) mc.player.yaw += 0.5;
                if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) mc.player.pitch -= 0.5;
                if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) mc.player.pitch += 0.5;
            }

            mc.player.pitch = Utils.clamp(mc.player.pitch, -90, 90);
        }
    }

    private void tickSneakJumpAndSprint() {
        mc.player.input.jumping = jump.get() && Input.isPressed(mc.options.keyJump);
        mc.player.input.sneaking = sneak.get() && Input.isPressed(mc.options.keySneak);
        mc.player.setSprinting(sprint.get() && Input.isPressed(mc.options.keySprint));
    }

    private boolean skip() {
        return mc.currentScreen == null || Modules.get().isActive(Freecam.class) || (mc.currentScreen instanceof CreativeInventoryScreen && ((CreativeInventoryScreenAccessor) mc.currentScreen).getSelectedTab() == ItemGroup.SEARCH.getIndex()) || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen || mc.currentScreen instanceof AnvilScreen || mc.currentScreen instanceof AbstractCommandBlockScreen || mc.currentScreen instanceof StructureBlockScreen;
    }
}
