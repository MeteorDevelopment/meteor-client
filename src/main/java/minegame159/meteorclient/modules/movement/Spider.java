package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {
    private static DoubleSetting speed = new DoubleSetting("speed", "Speed.", 0.2, 0.0, null);

    public Spider() {
        super(Category.Movement, "spider", "Allows you to climb walls.", speed);
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (!mc.player.horizontalCollision) return;

        Vec3d velocity = mc.player.getVelocity();
        if (velocity.y >= 0.2) return;

        mc.player.setVelocity(velocity.x, speed.value, velocity.z);
    }
}
