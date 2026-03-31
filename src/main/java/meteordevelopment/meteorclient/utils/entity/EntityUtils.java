// TODO(Ravel): Failed to fully resolve file: null cannot be cast to non-null type com.intellij.psi.PsiClass
/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import meteordevelopment.meteorclient.mixin.*;
import meteordevelopment.meteorclient.mixin.DirectionAccessor;
import meteordevelopment.meteorclient.mixin.EntitySectionStorageAccessor;
import meteordevelopment.meteorclient.mixin.LevelAccessor;
import meteordevelopment.meteorclient.mixin.LevelEntityGetterAdapterAccessor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityUtils {
    private static final BlockPos.MutableBlockPos testPos = new BlockPos.Mutable();

    private EntityUtils() {
    }

    public static boolean isAttackable(EntityType<?> type) {
        return type != EntityType.AREA_EFFECT_CLOUD && type != EntityType.ARROW && type != EntityType.FALLING_BLOCK && type != EntityType.FIREWORK_ROCKET && type != EntityType.ITEM && type != EntityType.LLAMA_SPIT && type != EntityType.SPECTRAL_ARROW && type != EntityType.ENDER_PEARL && type != EntityType.EXPERIENCE_BOTTLE && type != EntityType.SPLASH_POTION && type != EntityType.LINGERING_POTION && type != EntityType.TRIDENT && type != EntityType.LIGHTNING_BOLT && type != EntityType.FISHING_BOBBER && type != EntityType.EXPERIENCE_ORB && type != EntityType.EGG;
    }

    public static boolean isRideable(EntityType<?> type) {
        return type == EntityType.PIG ||
            type == EntityType.STRIDER ||
            type == EntityType.HORSE ||
            type == EntityType.DONKEY ||
            type == EntityType.MULE ||
            type == EntityType.SKELETON_HORSE ||
            type == EntityType.ZOMBIE_HORSE ||
            type == EntityType.LLAMA ||
            type == EntityType.TRADER_LLAMA ||
            type == EntityType.CAMEL ||
            type == EntityType.CAMEL_HUSK ||
            type == EntityType.MINECART ||
            type == EntityType.OAK_BOAT ||
            type == EntityType.SPRUCE_BOAT ||
            type == EntityType.BIRCH_BOAT ||
            type == EntityType.JUNGLE_BOAT ||
            type == EntityType.ACACIA_BOAT ||
            type == EntityType.CHERRY_BOAT ||
            type == EntityType.DARK_OAK_BOAT ||
            type == EntityType.PALE_OAK_BOAT ||
            type == EntityType.MANGROVE_BOAT ||
            type == EntityType.BAMBOO_RAFT ||
            type == EntityType.ACACIA_CHEST_BOAT ||
            type == EntityType.BIRCH_CHEST_BOAT ||
            type == EntityType.CHERRY_CHEST_BOAT ||
            type == EntityType.DARK_OAK_CHEST_BOAT ||
            type == EntityType.JUNGLE_CHEST_BOAT ||
            type == EntityType.MANGROVE_CHEST_BOAT ||
            type == EntityType.OAK_CHEST_BOAT ||
            type == EntityType.PALE_OAK_CHEST_BOAT ||
            type == EntityType.SPRUCE_CHEST_BOAT ||
            type == EntityType.BAMBOO_CHEST_RAFT ||
            type == EntityType.NAUTILUS ||
            type == EntityType.ZOMBIE_NAUTILUS ||
            type == EntityType.HAPPY_GHAST;
    }

    public static float getTotalHealth(LivingEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    public static int getPing(Player player) {
        if (mc.getNetworkHandler() == null) return 0;

        PlayerInfo playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return 0;
        return playerListEntry.getLatency();
    }

    public static GameType getGameMode(Player player) {
        if (player == null) return null;
        PlayerInfo playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return null;
        return playerListEntry.getGameMode();
    }

    @SuppressWarnings("deprecation") // Use of AbstractBlock.AbstractBlockState#blocksMovement
    public static boolean isAboveWater(BlockEntity entity) {
        BlockPos.MutableBlockPos blockPos = entity.getBlockPos().mutableCopy();
        int bottom = mc.world.getBottomY();

        while (blockPos.getY() > bottom) {
            BlockState state = mc.world.getBlockState(blockPos);

            if (state.blocksMovement()) break;

            Fluid fluid = state.getFluidState().getFluid();
            if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
                return true;
            }

            blockPos.move(0, -1, 0);
        }

        return false;
    }

    public static boolean isInCobweb(BlockEntity entity) {
        return mc.world.getStatesInBoxIfLoaded(entity.getBoundingBox()).anyMatch(state -> state.isOf(Blocks.COBWEB));
    }

    public static boolean isInRenderDistance(BlockEntity entity) {
        if (entity == null) return false;
        return isInRenderDistance(entity.getX(), entity.getZ());
    }

    public static boolean isInRenderDistance(BlockEntity entity) {
        if (entity == null) return false;
        return isInRenderDistance(entity.getPos().getX(), entity.getPos().getZ());
    }

    public static boolean isInRenderDistance(BlockPos pos) {
        if (pos == null) return false;
        return isInRenderDistance(pos.getX(), pos.getZ());
    }

    public static boolean isInRenderDistance(double posX, double posZ) {
        double x = Math.abs(mc.gameRenderer.getCamera().getCameraPos().x - posX);
        double z = Math.abs(mc.gameRenderer.getCamera().getCameraPos().z - posZ);
        double d = (mc.options.getViewDistance().getValue() + 1) * 16;

        return x < d && z < d;
    }

    public static BlockPos getCityBlock(Player player) {
        if (player == null) return null;

        double bestDistanceSquared = 6 * 6;
        Direction bestDirection = null;

        for (Direction direction : DirectionAccessor.meteor$getHorizontal()) {
            testPos.set(player.getBlockPos().offset(direction));

            BlockBehaviour block = mc.world.getBlockState(testPos).getBlock();
            if (block != Blocks.OBSIDIAN && block != Blocks.NETHERITE_BLOCK && block != Blocks.CRYING_OBSIDIAN
                && block != Blocks.RESPAWN_ANCHOR && block != Blocks.ANCIENT_DEBRIS) continue;

            double testDistanceSquared = PlayerUtils.squaredDistanceTo(testPos);
            if (testDistanceSquared < bestDistanceSquared) {
                bestDistanceSquared = testDistanceSquared;
                bestDirection = direction;
            }
        }

        if (bestDirection == null) return null;
        return player.getBlockPos().offset(bestDirection);
    }

    public static String getName(BlockEntity entity) {
        if (entity == null) return null;
        if (entity instanceof Player) return entity.getName().getString();
        return entity.getType().getName().getString();
    }

    public static Color getColorFromDistance(BlockEntity entity) {
        // Credit to Icy from Stackoverflow
        Color distanceColor = new Color(255, 255, 255);
        double distance = PlayerUtils.distanceToCamera(entity);
        double percent = distance / 60;

        if (percent < 0 || percent > 1) {
            distanceColor.set(0, 255, 0, 255);
            return distanceColor;
        }

        int r, g;

        if (percent < 0.5) {
            r = 255;
            g = (int) (255 * percent / 0.5);  // Closer to 0.5, closer to yellow (255,255,0)
        } else {
            g = 255;
            r = 255 - (int) (255 * (percent - 0.5) / 0.5); // Closer to 1.0, closer to green (0,255,0)
        }

        distanceColor.set(r, g, 0, 255);
        return distanceColor;
    }

    public static Color getColorFromHealth(BlockEntity entity, Color nonLivingEntityColor) {
        // For entities without health (items, pearls, etc.)
        if (!(entity instanceof LivingEntity living)) {
            return new Color(nonLivingEntityColor);
        }

        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();

        if (maxHealth <= 0) {
            return new Color(nonLivingEntityColor);
        }

        double percent = health / maxHealth;

        percent = Math.max(0.0, Math.min(1.0, percent));

        int r, g;

        if (percent < 0.5) {
            // Red to Yellow
            r = 255;
            g = (int) (255 * (percent / 0.5));
        } else {
            // Yellow to Green
            g = 255;
            r = 255 - (int) (255 * ((percent - 0.5) / 0.5));
        }

        return new Color(r, g, 0, 255);
    }

    public static boolean intersectsWithEntity(AABB box, Predicate<BlockEntity> predicate) {
        LevelEntityGetter<BlockEntity> entityLookup = ((LevelAccessor) mc.world).meteor$getEntityLookup();

        // Fast implementation using SimpleEntityLookup that returns on the first intersecting entity
        if (entityLookup instanceof LevelEntityGetterAdapter<BlockEntity> simpleEntityLookup) {
            EntitySectionStorage<BlockEntity> cache = ((LevelEntityGetterAdapterAccessor) simpleEntityLookup).meteor$getSectionStorage();
            LongSortedSet trackedPositions = ((EntitySectionStorageAccessor) cache).meteor$getSectionIds();
            Long2ObjectMap<EntitySection<BlockEntity>> trackingSections = ((EntitySectionStorageAccessor) cache).meteor$getSections();

            int i = SectionPos.getSectionCoord(box.minX - 2);
            int j = SectionPos.getSectionCoord(box.minY - 2);
            int k = SectionPos.getSectionCoord(box.minZ - 2);
            int l = SectionPos.getSectionCoord(box.maxX + 2);
            int m = SectionPos.getSectionCoord(box.maxY + 2);
            int n = SectionPos.getSectionCoord(box.maxZ + 2);

            for (int o = i; o <= l; o++) {
                long p = SectionPos.asLong(o, 0, 0);
                long q = SectionPos.asLong(o, -1, -1);
                LongBidirectionalIterator longIterator = trackedPositions.subSet(p, q + 1).iterator();

                while (longIterator.hasNext()) {
                    long r = longIterator.nextLong();
                    int s = SectionPos.unpackY(r);
                    int t = SectionPos.unpackZ(r);

                    if (s >= j && s <= m && t >= k && t <= n) {
                        EntitySection<BlockEntity> entityTrackingSection = trackingSections.get(r);

                        if (entityTrackingSection != null && entityTrackingSection.getStatus().shouldTrack()) {
                            for (BlockEntity entity : ((EntityTrackingSectionAccessor) entityTrackingSection).<BlockEntity>meteor$getCollection()) {
                                if (entity.getBoundingBox().intersects(box) && predicate.test(entity)) return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        // Slow implementation that loops every entity if for some reason the EntityLookup implementation is changed
        AtomicBoolean found = new AtomicBoolean(false);

        entityLookup.forEachIntersects(box, entity -> {
            if (!found.get() && predicate.test(entity)) found.set(true);
        });

        return found.get();
    }

    public static EntityType<?> getGroup(BlockEntity entity) {
        return entity.getType();
    }

    // Copied from ServerPlayNetworkHandler#isEntityOnAir
    public static boolean isOnAir(BlockEntity entity) {
        return entity.getEntityWorld().getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0)).allMatch(BlockBehaviour.BlockStateBase::isAir);
    }
}
