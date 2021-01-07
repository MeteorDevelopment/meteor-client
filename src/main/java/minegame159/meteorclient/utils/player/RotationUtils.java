package minegame159.meteorclient.utils.player;

import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtils {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static void packetRotate(Entity entity) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(getNeededYaw(entity.getPos()), getNeededPitch(entity.getPos()), mc.player.isOnGround()));
    }

    public static void packetRotate(BlockPos blockPos) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(getNeededYaw(Utils.vec3d(blockPos)), getNeededPitch(Utils.vec3d(blockPos)), mc.player.isOnGround()));
    }

    public static void packetRotate(Vec3d vec) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(getNeededYaw(vec), getNeededPitch(vec), mc.player.isOnGround()));
    }

    public static void packetRotate(float yaw, float pitch) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(yaw, pitch, mc.player.isOnGround()));
    }

    public static float getNeededYaw(Vec3d vec) {
        return mc.player.yaw + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), vec.x - mc.player.getX())) - 90f - mc.player.yaw);
    }

    public static float getNeededPitch(Vec3d vec) {
        double diffX = vec.x - mc.player.getX();
        double diffY = vec.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = vec.z - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.pitch + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.pitch);
    }
}
