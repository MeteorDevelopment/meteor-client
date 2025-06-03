/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.option.KeyBinding;

public class AutoWalk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Walking mode.")
        .defaultValue(Mode.Smart)
        .onChanged(mode1 -> {
            if (isActive() && Utils.canUpdate()) {
                if (mode1 == Mode.Simple) {
                    PathManagers.get().stop();
                } else {
                    createGoal();
                }

                unpress();
            }
        })
        .build()
    );

    private final Setting<Direction> direction = sgGeneral.add(new EnumSetting.Builder<Direction>()
        .name("simple-direction")
        .description("The direction to walk in simple mode.")
        .defaultValue(Direction.Forwards)
        .onChanged(direction1 -> {
            if (isActive()) unpress();
        })
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> disableOnInput = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-input")
        .description("Disable module on manual movement input")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> disableOnY = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-y-change")
        .description("Disable module if player moves vertically")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> waitForChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("no-unloaded-chunks")
        .description("Do not allow movement into unloaded chunks")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    public AutoWalk() {
        super(Categories.Movement, "auto-walk", "Automatically walks forward.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Smart) createGoal();
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Simple) unpress();
        else PathManagers.get().stop();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mode.get() == Mode.Simple) {
            if (disableOnY.get() && mc.player.lastY != mc.player.getY()) {
                toggle();
                return;
            }

            switch (direction.get()) {
                case Forwards -> setPressed(mc.options.forwardKey, true);
                case Backwards -> setPressed(mc.options.backKey, true);
                case Left -> setPressed(mc.options.leftKey, true);
                case Right -> setPressed(mc.options.rightKey, true);
            }
        } else {
            if (PathManagers.get() instanceof NopPathManager) {
                info("Smart mode requires Baritone");
                toggle();
            }
        }
    }

    private void onMovement() {
        if (!disableOnInput.get()) return;
        if (mc.currentScreen != null) {
            GUIMove guiMove = Modules.get().get(GUIMove.class);
            if (!guiMove.isActive()) return;
            if (guiMove.skip()) return;
        }
        toggle();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (isMovementKey(event.key) && event.action == KeyAction.Press) onMovement();
    }

     @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (isMovementButton(event.button) && event.action == KeyAction.Press) onMovement();
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mode.get() == Mode.Simple && waitForChunks.get()) {
            int chunkX = (int) ((mc.player.getX() + event.movement.x * 2) / 16);
            int chunkZ = (int) ((mc.player.getZ() + event.movement.z * 2) / 16);
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                ((IVec3d) event.movement).meteor$set(0, event.movement.y, 0);
            }
        }
    }

    private void unpress() {
        setPressed(mc.options.forwardKey, false);
        setPressed(mc.options.backKey, false);
        setPressed(mc.options.leftKey, false);
        setPressed(mc.options.rightKey, false);
    }

    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    private boolean isMovementKey(int key) {
        return mc.options.forwardKey.matchesKey(key, 0)
            || mc.options.backKey.matchesKey(key, 0)
            || mc.options.leftKey.matchesKey(key, 0)
            || mc.options.rightKey.matchesKey(key, 0)
            || mc.options.sneakKey.matchesKey(key, 0)
            || mc.options.jumpKey.matchesKey(key, 0);
    }

    private boolean isMovementButton(int button) {
        return mc.options.forwardKey.matchesMouse(button)
            || mc.options.backKey.matchesMouse(button)
            || mc.options.leftKey.matchesMouse(button)
            || mc.options.rightKey.matchesMouse(button)
            || mc.options.sneakKey.matchesMouse(button)
            || mc.options.jumpKey.matchesMouse(button);
    }

    private void createGoal() {
        PathManagers.get().moveInDirection(mc.player.getYaw());
    }

    public enum Mode {
        Simple,
        Smart
    }

    public enum Direction {
        Forwards,
        Backwards,
        Left,
        Right
    }
}
