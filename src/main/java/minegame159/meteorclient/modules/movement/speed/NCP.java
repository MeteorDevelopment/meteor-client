package minegame159.meteorclient.modules.movement.speed;

import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.utils.misc.Vector2;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.effect.StatusEffects;

public class NCP extends SpeedMode {

    public NCP() {
        super(SpeedModes.NCP);
        stage = 1;
        moveSpeed = 0.2873D;
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        switch (stage) {
            case 0: {
                ++stage;
                lastDist = 0.0;
                break;
            }
            case 2: {
                double motionY = 0.40123128;
                if (mc.player.forwardSpeed == 0.0f && mc.player.sidewaysSpeed == 0.0f || !mc.player.isOnGround()) break;
                if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) motionY += (float)(mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1f;

                ((IVec3d) event.movement).setY(motionY);
                moveSpeed *= speed.ncpSpeed.get();
                break;
            }
            case 3: {
                moveSpeed = lastDist - 0.76 * (lastDist - getDefaultSpeed());
                break;
            }
            default: {
                if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision && stage > 0) {
                    stage = PlayerUtils.isMoving() ? 1 : 0;
                }
                moveSpeed = lastDist - lastDist / 159.0;
            }
        }
        moveSpeed = Math.max(moveSpeed, getDefaultSpeed());

        Vector2 change = PlayerUtils.transformStrafe(moveSpeed);
        ((IVec3d) event.movement).setXZ(change.x, change.y);

        ++stage;
    }

    @Override
    public void onTick(TickEvent.Pre event) {
        lastDist = Math.sqrt(
                (mc.player.getX() - mc.player.prevX) * (mc.player.getX() - mc.player.prevX)
                + (mc.player.getZ() - mc.player.prevZ) * (mc.player.getZ() - mc.player.prevZ)
        );
    }
}
