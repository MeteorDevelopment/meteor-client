package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.DoubleSettingBuilder;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {
    private Setting<Double> speed = addSetting(new DoubleSettingBuilder()
            .name("speed")
            .description("Speed.")
            .defaultValue(0.2)
            .min(0.0)
            .build()
    );

    public Spider() {
        super(Category.Movement, "spider", "Allows you to climb walls.");
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (!mc.player.horizontalCollision) return;

        Vec3d velocity = mc.player.getVelocity();
        if (velocity.y >= 0.2) return;

        mc.player.setVelocity(velocity.x, speed.value(), velocity.z);
    }
}
