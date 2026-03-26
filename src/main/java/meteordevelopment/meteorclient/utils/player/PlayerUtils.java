/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.text.TextUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.WHITE;

public class PlayerUtils {
    private static final double diagonal = 1 / Math.sqrt(2);
    private static final Vec3 horizontalVelocity = new Vec3(0, 0, 0);

    private static final Color color = new Color();

    private PlayerUtils() {
    }

    public static Color getPlayerColor(Player entity, Color defaultColor) {
        if (Friends.get().isFriend(entity)) {
            return color.set(Config.get().friendColor.get()).a(defaultColor.a);
        }

        if (Config.get().useTeamColor.get() && !color.set(TextUtils.getMostPopularColor(entity.getDisplayName())).equals(WHITE)) {
            return color.a(defaultColor.a);
        }

        return defaultColor;
    }

    public static Vec3 getHorizontalVelocity(double bps) {
        float yaw = mc.player.getYRot();

        if (PathManagers.get().isPathing()) {
            yaw = PathManagers.get().getTargetYaw();
        }

        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 right = Vec3.directionFromRotation(0, yaw + 90);
        double velX = 0;
        double velZ = 0;

        boolean a = false;
        if (mc.player.input.keyPresses.forward()) {
            velX += forward.x / 20 * bps;
            velZ += forward.z / 20 * bps;
            a = true;
        }
        if (mc.player.input.keyPresses.backward()) {
            velX -= forward.x / 20 * bps;
            velZ -= forward.z / 20 * bps;
            a = true;
        }

        boolean b = false;
        if (mc.player.input.keyPresses.right()) {
            velX += right.x / 20 * bps;
            velZ += right.z / 20 * bps;
            b = true;
        }
        if (mc.player.input.keyPresses.left()) {
            velX -= right.x / 20 * bps;
            velZ -= right.z / 20 * bps;
            b = true;
        }

        if (a && b) {
            velX *= diagonal;
            velZ *= diagonal;
        }

        ((IVec3d) horizontalVelocity).meteor$setXZ(velX, velZ);
        return horizontalVelocity;
    }

    public static void centerPlayer() {
        double x = Mth.floor(mc.player.getX()) + 0.5;
        double z = Mth.floor(mc.player.getZ()) + 0.5;
        mc.player.setPos(x, mc.player.getY(), z);
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.onGround(), mc.player.horizontalCollision));
    }

    @SuppressWarnings("DataFlowIssue")
    public static boolean canSeeEntity(Entity entity) {
        Vec3 vec1 = new Vec3(0, 0, 0);
        Vec3 vec2 = new Vec3(0, 0, 0);

        ((IVec3d) vec1).meteor$set(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ());
        ((IVec3d) vec2).meteor$set(entity.getX(), entity.getY(), entity.getZ());
        boolean canSeeFeet = mc.level.clip(new ClipContext(vec1, vec2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;

        ((IVec3d) vec2).meteor$set(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
        boolean canSeeEyes = mc.level.clip(new ClipContext(vec1, vec2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;

        return canSeeFeet || canSeeEyes;
    }

    public static float[] calculateAngle(Vec3 target) {
        Vec3 eyesPos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        double dX = target.x - eyesPos.x;
        double dY = (target.y - eyesPos.y) * -1.0D;
        double dZ = target.z - eyesPos.z;

        double dist = Math.sqrt(dX * dX + dZ * dZ);

        return new float[]{(float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dZ, dX)) - 90.0D), (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dY, dist)))};
    }

    public static boolean shouldPause(boolean ifBreaking, boolean ifEating, boolean ifDrinking) {
        if (ifBreaking && mc.gameMode.isDestroying()) return true;
        if (ifEating && (mc.player.isUsingItem() && (mc.player.getMainHandItem().getItem().components().has(DataComponents.FOOD) || mc.player.getOffhandItem().getItem().components().has(DataComponents.FOOD)))) return true;
        return ifDrinking && (mc.player.isUsingItem() && (mc.player.getMainHandItem().getItem() instanceof PotionItem || mc.player.getOffhandItem().getItem() instanceof PotionItem));
    }

    public static boolean isMoving() {
        return mc.player.zza != 0 || mc.player.xxa != 0;
    }

    public static boolean isSprinting() {
        return mc.player.isSprinting() && (mc.player.zza != 0 || mc.player.xxa != 0);
    }

    public static boolean isInHole(boolean doubles) {
        if (!Utils.canUpdate()) return false;

        BlockPos blockPos = mc.player.blockPosition();
        int air = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            BlockState state = mc.level.getBlockState(blockPos.relative(direction));

            if (state.getBlock().getExplosionResistance() < 600) {
                if (!doubles || direction == Direction.DOWN) return false;

                air++;

                for (Direction dir : Direction.values()) {
                    if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                    BlockState blockState1 = mc.level.getBlockState(blockPos.relative(direction).relative(dir));

                    if (blockState1.getBlock().getExplosionResistance() < 600) {
                        return false;
                    }
                }
            }
        }

        return air < 2;
    }

    public static float possibleHealthReductions() {
        return possibleHealthReductions(true, true);
    }

    public static float possibleHealthReductions(boolean entities, boolean fall) {
        float damageTaken = 0;

        if (entities) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                // Check for end crystals
                if (entity instanceof EndCrystal) {
                    float crystalDamage = DamageUtils.crystalDamage(mc.player, entity.position());
                    if (crystalDamage > damageTaken) damageTaken = crystalDamage;
                }
                // Check for players holding swords
                else if (entity instanceof Player player && !Friends.get().isFriend(player) && isWithin(entity, 5)) {
                    float attackDamage = DamageUtils.getAttackDamage(player, mc.player);
                    if (attackDamage > damageTaken) damageTaken = attackDamage;
                }
            }

            // Check for beds if in nether
            if (mc.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.BED_RULE).explodes()) {
                for (BlockEntity blockEntity : Utils.blockEntities()) {
                    BlockPos bp = blockEntity.getBlockPos();
                    Vec3 pos = new Vec3(bp.getX(), bp.getY(), bp.getZ());

                    if (blockEntity instanceof BedBlockEntity) {
                        float explosionDamage = DamageUtils.bedDamage(mc.player, pos);
                        if (explosionDamage > damageTaken) damageTaken = explosionDamage;
                    }
                }
            }
        }

        // Check for fall distance with water check
        if (fall) {
            if (!Modules.get().isActive(NoFall.class) && mc.player.fallDistance > 3) {
                float damage = DamageUtils.fallDamage(mc.player);

                if (damage > damageTaken && !EntityUtils.isAboveWater(mc.player)) {
                    damageTaken = damage;
                }
            }
        }

        return damageTaken;
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(squaredDistance(x1, y1, z1, x2, y2, z2));
    }

    public static double distanceTo(Entity entity) {
        return distanceTo(entity.getX(), entity.getY(), entity.getZ());
    }

    public static double distanceTo(BlockPos blockPos) {
        return distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static double distanceTo(Vec3 vec3d) {
        return distanceTo(vec3d.x(), vec3d.y(), vec3d.z());
    }

    public static double distanceTo(double x, double y, double z) {
        return Math.sqrt(squaredDistanceTo(x, y, z));
    }

    public static double squaredDistanceTo(Entity entity) {
        return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
    }

    public static double squaredDistanceTo(BlockPos blockPos) {
        return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static double squaredDistanceTo(double x, double y, double z) {
        return squaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), x, y, z);
    }

    public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double f = x1 - x2;
        double g = y1 - y2;
        double h = z1 - z2;
        return org.joml.Math.fma(f, f, org.joml.Math.fma(g, g, h * h));
    }

    public static boolean isWithin(Entity entity, double r) {
        return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ()) <= r * r;
    }

    public static boolean isWithin(Vec3 vec3d, double r) {
        return squaredDistanceTo(vec3d.x(), vec3d.y(), vec3d.z()) <= r * r;
    }

    public static boolean isWithin(BlockPos blockPos, double r) {
        return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ()) <= r * r;
    }

    public static boolean isWithin(double x, double y, double z, double r) {
        return squaredDistanceTo(x, y, z) <= r * r;
    }

    public static double distanceToCamera(double x, double y, double z) {
        return Math.sqrt(squaredDistanceToCamera(x, y, z));
    }

    public static double distanceToCamera(Entity entity) {
        return distanceToCamera(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ());
    }

    public static double squaredDistanceToCamera(double x, double y, double z) {
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        return squaredDistance(cameraPos.x, cameraPos.y, cameraPos.z, x, y, z);
    }

    public static double squaredDistanceToCamera(Entity entity) {
        return squaredDistanceToCamera(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ());
    }

    public static boolean isWithinCamera(Entity entity, double r) {
        return squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()) <= r * r;
    }

    public static boolean isWithinCamera(Vec3 vec3d, double r) {
        return squaredDistanceToCamera(vec3d.x(), vec3d.y(), vec3d.z()) <= r * r;
    }

    public static boolean isWithinCamera(BlockPos blockPos, double r) {
        return squaredDistanceToCamera(blockPos.getX(), blockPos.getY(), blockPos.getZ()) <= r * r;
    }

    public static boolean isWithinCamera(double x, double y, double z, double r) {
        return squaredDistanceToCamera(x, y, z) <= r * r;
    }

    public static boolean isWithinReach(Entity entity) {
        return isWithinReach(entity.getX(), entity.getY(), entity.getZ());
    }

    public static boolean isWithinReach(Vec3 vec3d) {
        return isWithinReach(vec3d.x(), vec3d.y(), vec3d.z());
    }

    public static boolean isWithinReach(BlockPos blockPos) {
        return isWithinReach(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static boolean isWithinReach(double x, double y, double z) {
        return squaredDistance(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), x, y, z) <= mc.player.blockInteractionRange() * mc.player.blockInteractionRange();
    }

    public static Dimension getDimension() {
        if (mc.level == null) return Dimension.Overworld;

        return switch (mc.level.dimension().identifier().getPath()) {
            case "the_nether" -> Dimension.Nether;
            case "the_end" -> Dimension.End;
            default -> Dimension.Overworld;
        };
    }

    public static GameType getGameMode() {
        if (mc.player == null) return null;
        PlayerInfo playerListEntry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (playerListEntry == null) return null;
        return playerListEntry.getGameMode();
    }

    public static float getTotalHealth() {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    public static boolean isAlive() {
        return mc.player.isAlive() && !mc.player.isDeadOrDying();
    }

    public static int getPing() {
        if (mc.getConnection() == null) return 0;

        PlayerInfo playerListEntry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (playerListEntry == null) return 0;
        return playerListEntry.getLatency();
    }
}
