package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.DoubleSettingBuilder;
import net.minecraft.util.math.Vec3d;

public class FastLadder extends Module {
    private Setting<Double> speed = addSetting(new DoubleSettingBuilder()
            .name("speed")
            .description("Speed.")
            .defaultValue(0.2872)
            .min(0.0)
            .build()
    );

    public FastLadder() {
        super(Category.Movement, "fast-ladder", "Climb ladders faster.");
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (!mc.player.isClimbing() || !mc.player.horizontalCollision) return;
        if (mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) return;

        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, speed.value(), velocity.z);
    }
}
