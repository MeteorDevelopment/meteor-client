package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.util.math.Vec3d;

public class FastLadder extends Module {
    private Setting<Double> speed = addSetting(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed.")
            .defaultValue(0.2872)
            .min(0.0)
            .build()
    );

    public FastLadder() {
        super(Category.Movement, "fast-ladder", "Climb ladders faster.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (!mc.player.isClimbing() || !mc.player.horizontalCollision) return;
        if (mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) return;

        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, speed.get(), velocity.z);
    });
}
