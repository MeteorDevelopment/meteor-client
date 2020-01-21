package minegame159.meteorclient.modules.movement;

import baritone.api.BaritoneAPI;
import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.utils.GoalDirection;
import minegame159.meteorclient.utils.KeyBindings;

public class AutoWalk extends Module {
    public enum Mode {
        Simple,
        Smart
    }

    private static EnumSetting<Mode> mode = new EnumSetting<>("mode", "Walking mode.", Mode.Smart);
    private static BoolSetting jump = new BoolSetting("jump", "Jump over blocks.", true);

    private int jumpTimer = 0;
    private int timer = 0;
    private GoalDirection goal;

    public AutoWalk() {
        super(Category.Movement, "auto-walk", "Automatically walks forward.", mode, jump);
    }

    @Override
    public void onActivate() {
        if (mode.value == Mode.Smart) {
            timer = 0;
            goal = new GoalDirection(mc.player.getPos(), mc.player.yaw);
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.value == Mode.Simple) KeyBindings.forward.setPressed(false);
        else BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (mode.value == Mode.Simple) {
            KeyBindings.forward.setPressed(true);

            if (jump.value && jumpTimer >= 9 && mc.world.getBlockState(mc.player.getBlockPos().add(mc.player.getMovementDirection().getVector())).getMaterial().blocksMovement()) {
                mc.player.jump();
                jumpTimer = 0;
            }

            jumpTimer++;
        } else {
            if (timer > 20) {
                timer = 0;
                goal.recalculate(mc.player.getPos());
            }

            timer++;
        }
    }
}
