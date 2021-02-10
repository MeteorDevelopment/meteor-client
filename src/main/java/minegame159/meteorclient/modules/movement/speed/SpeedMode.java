package minegame159.meteorclient.modules.movement.speed;

import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.movement.Speed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SpeedMode {
    protected final MinecraftClient mc;
    public final SpeedModes type;
    protected final Speed speed;

    public double moveSpeed;
    public int stage;
    public int ticks;
    public double lastDist;
    public int timerDelay;

    public SpeedMode(SpeedModes type) {
        this.speed = Modules.get().get(Speed.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onTick(TickEvent.Pre event) {}

    public void onMove(PlayerMoveEvent event) {}

    public void onRubberband() {
        stage = 4;
        timerDelay = 0;
        moveSpeed = 0.2873;
        lastDist = 0;
    }

    public void onActivate() {}

    public void onDeactivate() {}

    public double getDefaultSpeed() {
        double defaultSpeed = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        return defaultSpeed;
    }

    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}