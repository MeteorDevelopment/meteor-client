/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.mixininterface.ICreativeInventoryScreen;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.render.Freecam;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Input;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.item.ItemGroup;
import org.lwjgl.glfw.GLFW;

public class InvMove extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
            .name("sneak")
            .description("Allows you to sneak.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> jump = sgGeneral.add(new BoolSetting.Builder()
            .name("jump")
            .description("Allows you to jump.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
            .name("sprint")
            .description("Allows you to sprint.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> arrowsRotate = sgGeneral.add(new BoolSetting.Builder()
            .name("arrows-rotate")
            .description("Allows you to use arrow keys to rotate.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotate-speed")
            .description("Rotation speed.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    public InvMove() {
        super(Category.Movement, "inv-move", "Allows you to move while in guis.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (!skip()) tickSneakJumpAndSprint();
    });

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
            if (Input.isPressed(GLFW.GLFW_KEY_RIGHT)) mc.player.yaw += rotateSpeed.get();
            if (Input.isPressed(GLFW.GLFW_KEY_LEFT)) mc.player.yaw -= rotateSpeed.get();
            if (Input.isPressed(GLFW.GLFW_KEY_UP)) mc.player.pitch -= rotateSpeed.get();
            if (Input.isPressed(GLFW.GLFW_KEY_DOWN)) mc.player.pitch += rotateSpeed.get();

            mc.player.pitch = Utils.clamp(mc.player.pitch, -90, 90);
        }
    }

    private void tickSneakJumpAndSprint() {
        mc.player.input.jumping = jump.get() && Input.isPressed(mc.options.keyJump);
        mc.player.input.sneaking = sneak.get() && Input.isPressed(mc.options.keySneak);
        mc.player.setSprinting(sprint.get() && Input.isPressed(mc.options.keySprint));
    }

    private boolean skip() {
        return mc.currentScreen == null || ModuleManager.INSTANCE.isActive(Freecam.class) || (mc.currentScreen instanceof CreativeInventoryScreen && ((ICreativeInventoryScreen) mc.currentScreen).getSelectedTab() == ItemGroup.SEARCH.getIndex()) || mc.currentScreen instanceof WidgetScreen || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen || mc.currentScreen instanceof AnvilScreen;
    }
}
