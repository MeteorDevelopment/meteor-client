package minegame159.meteorclient.modules.movement.speed;

import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.movement.Speed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public class SpeedMode {
    protected final MinecraftClient mc;
    protected final Speed settings;
    private final SpeedModes type;

    public int stage;
    public double distance;
    public double speed;

    public SpeedMode(SpeedModes type) {
        this.settings = Modules.get().get(Speed.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;

        reset();
    }

    public void onTick() {
        distance = Math.sqrt((mc.player.getX() - mc.player.prevX) * (mc.player.getX() - mc.player.prevX) + (mc.player.getZ() - mc.player.prevZ) * (mc.player.getZ() - mc.player.prevZ));
    }

    public void onMove(PlayerMoveEvent event) {}

    public void onRubberband() {
        reset();
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

    private void reset() {
        stage = 0;
        distance = 0;
        speed = 0.2873;
    }

    public String getHudString() {
        return type.name();
    }
}