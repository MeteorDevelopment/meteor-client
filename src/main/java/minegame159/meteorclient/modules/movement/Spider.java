package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {
    private Setting<Double> speed = addSetting(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed.")
            .defaultValue(0.2)
            .min(0.0)
            .build()
    );

    public Spider() {
        super(Category.Movement, "spider", "Allows you to climb walls.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (!mc.player.horizontalCollision) return;

        Vec3d velocity = mc.player.getVelocity();
        if (velocity.y >= 0.2) return;

        mc.player.setVelocity(velocity.x, speed.get(), velocity.z);
    });
}
