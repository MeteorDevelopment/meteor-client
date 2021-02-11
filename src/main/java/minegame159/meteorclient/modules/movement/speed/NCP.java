package minegame159.meteorclient.modules.movement.speed;

import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.movement.Anchor;
import minegame159.meteorclient.utils.misc.Vector2;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class NCP extends SpeedMode {

    public NCP() {
        super(SpeedModes.NCP);
    }


    @Override
    public void onMove(PlayerMoveEvent event) {
        switch (stage) {
            case 0: //Reset
                if (PlayerUtils.isMoving()) {
                    stage++;
                    speed = 1.18f * getDefaultSpeed() - 0.01;
                }
            case 1: //Jump
                if (!PlayerUtils.isMoving() || !mc.player.isOnGround()) break;

                double motionY = 0.40123128;

                StatusEffectInstance jumpBoost = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) ? mc.player.getStatusEffect(StatusEffects.JUMP_BOOST) : null;
                if (jumpBoost != null) motionY += jumpBoost.getAmplifier() + 1 * 0.1f;

                ((IVec3d) event.movement).setY(motionY);
                speed *= settings.ncpSpeed.get();

                stage++;
                break;
            case 2: speed = distance - 0.76 * (distance - getDefaultSpeed()); stage++; break; //Slowdown after jump
            case 3: //Reset on collision or predict and update speed
                if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision && stage > 0) {
                    stage = 0;
                }
                speed = distance - (distance / 159.0);
                break;
        }

        speed = Math.max(speed, getDefaultSpeed());
        Vector2 change = PlayerUtils.transformStrafe(speed);

        double velX = change.x;
        double velZ = change.y;

        Anchor anchor = Modules.get().get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            velX = anchor.deltaX;
            velZ = anchor.deltaZ;
        }

        ((IVec3d) event.movement).setXZ(velX, velZ);
    }
}
