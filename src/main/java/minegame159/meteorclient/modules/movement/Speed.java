package minegame159.meteorclient.modules.movement;

import baritone.api.BaritoneAPI;
import baritone.behavior.LookBehavior;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PlayerMoveEvent;
import minegame159.meteorclient.mixininterface.ILookBehavior;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class Speed extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Multiplier, 1 equals default sprinting speed.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Use speed only when on ground.")
            .defaultValue(false)
            .build()
    );

    private Setting<Boolean> inWater = sgGeneral.add(new BoolSetting.Builder()
            .name("in-water")
            .description("Use speed when in water.")
            .defaultValue(false)
            .build()
    );

    private Setting<Boolean> whenSneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("when-sneaking")
            .description("Use speed when sneaking.")
            .defaultValue(false)
            .build()
    );

    public Speed() {
        super(Category.Movement, "speed", "Speeeeeed.");
    }

    @EventHandler
    private Listener<PlayerMoveEvent> onPlayerMove = new Listener<>(event -> {
        if (event.type != MovementType.SELF || mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;
        if (!whenSneaking.get() && mc.player.isSneaking()) return;
        if (onlyOnGround.get() && !mc.player.onGround) return;
        if (!inWater.get() && mc.player.isTouchingWater()) return;

        float yaw = mc.player.yaw;
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            if (((ILookBehavior) BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior()).getTarget() != null) {
                yaw = ((ILookBehavior) BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior()).getTarget().getYaw();
            }
        }

        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        double velX = 0;
        double velZ = 0;

        boolean a = false;
        if (mc.player.input.pressingForward) {
            velX += forward.x * 0.28 * speed.get();
            velZ += forward.z * 0.28 * speed.get();
            a = true;
        }
        if (mc.player.input.pressingBack) {
            velX -= forward.x * 0.28 * speed.get();
            velZ -= forward.z * 0.28 * speed.get();
            a = true;
        }

        boolean b = false;
        if (mc.player.input.pressingRight) {
            velX += right.x * 0.28 * speed.get();
            velZ += right.z * 0.28 * speed.get();
            b = true;
        }
        if (mc.player.input.pressingLeft) {
            velX -= right.x * 0.28 * speed.get();
            velZ -= right.z * 0.28 * speed.get();
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }

        ((IVec3d) event.movement).set(velX, event.movement.y, velZ);
    });
}
