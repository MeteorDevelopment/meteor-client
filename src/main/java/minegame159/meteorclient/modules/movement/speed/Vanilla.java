package minegame159.meteorclient.modules.movement.speed;

import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.movement.Anchor;
import minegame159.meteorclient.modules.movement.AutoJump;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class Vanilla extends SpeedMode {
    public Vanilla() {
        super(SpeedModes.Vanilla);
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.speed.get());
        double velX = vel.getX();
        double velZ = vel.getZ();

        if (speed.applySpeedPotions.get() && mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            double value = (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1) * 0.205;
            velX += velX * value;
            velZ += velZ * value;
        }

        Anchor anchor = Modules.get().get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            velX = anchor.deltaX;
            velZ = anchor.deltaZ;
        }

        ((IVec3d) event.movement).set(velX, event.movement.y, velZ);
    }

    @Override
    public void onTick(TickEvent.Pre event) {
        if (speed.jump.get()) {
            if (!mc.player.isOnGround() || mc.player.isSneaking() || !jump()) return;
            if (speed.jumpMode.get() == AutoJump.Mode.Jump) mc.player.jump();
            else ((IVec3d) mc.player.getVelocity()).setY(speed.hopHeight.get());
        }
    }

    private boolean jump() {
        switch (speed.jumpIf.get()) {
            case Sprinting: return PlayerUtils.isSprinting();
            case Walking:   return PlayerUtils.isMoving();
            case Always:    return true;
            default:        return false;
        }
    }
}
