package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import net.minecraft.util.math.Vec3d;

public class FastLadder extends Module {
    private static DoubleSetting speed = new DoubleSetting("speed", "Speed.", 0.2872, 0.0, null);

    public FastLadder() {
        super(Category.Movement, "fast-ladder", "Climb ladders faster.", speed);
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (!mc.player.isClimbing() || !mc.player.horizontalCollision) return;
        if (mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) return;

        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, speed.value, velocity.z);
    }
}
