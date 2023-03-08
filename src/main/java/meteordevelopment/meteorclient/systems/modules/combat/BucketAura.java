/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import com.google.common.collect.Streams;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.HorizontalDirection;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class BucketAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWater = settings.createGroup("Water");
    private final SettingGroup sgLava = settings.createGroup("Lava");

    private final Setting<AuraPriority> priority = sgGeneral.add(new EnumSetting.Builder<AuraPriority>()
        .name("priority")
        .description("Which aura to be prioritised for targeting.")
        .defaultValue(AuraPriority.Water)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The maximum distance the entity can be to target it for each aura.")
        .defaultValue(5)
        .min(0)
        .sliderRange(0, 6)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Minimum delay in ticks for targeting entities for each aura.")
        .defaultValue(10)
        .min(0)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> collect = sgGeneral.add(new BoolSetting.Builder()
        .name("collect")
        .description("Allow Bucket Aura to collect fluids in empty bucket.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> collectDelay = sgGeneral.add(new IntSetting.Builder()
        .name("collect-delay")
        .description("The tick delay between collecting fluids.")
        .defaultValue(3)
        .min(0)
        .sliderRange(1, 20)
        .visible(collect::get)
        .build()
    );

    private final Setting<Boolean> ignoreBabies = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-babies")
        .description("If checked, babies will be ignored for each aura.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smashAdjacent = sgGeneral.add(new BoolSetting.Builder()
        .name("smash-adjacent")
        .description("Smashes insta breakable blocks around the target to allow fluid placement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiExplode = sgWater.add(new BoolSetting.Builder()
        .name("anti-explode")
        .description("Automatically places water/lava to prevent block damage from explosions.")
        .defaultValue(true)
        .build()
    );

    // Water Bucket

    private final Setting<Boolean> waterEnabled = sgWater.add(new BoolSetting.Builder()
        .name("water-bucket")
        .description("Whether or not to enable water-bucket aura.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> waterEntities = sgWater.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to target.")
        .defaultValue(EntityType.BLAZE, EntityType.ENDERMAN)
        .visible(waterEnabled::get)
        .build()
    );

    private final Setting<PlayerTargets> waterPlayerTargets = sgWater.add(new EnumSetting.Builder<PlayerTargets>()
        .name("player-targets")
        .description("Other players to be targeted.")
        .defaultValue(PlayerTargets._Friends)
        .visible(() -> waterEnabled.get() && waterEntities.get().getBoolean(EntityType.PLAYER))
        .build()
    );

    private final Setting<Boolean> waterExtinguish = sgWater.add(new BoolSetting.Builder()
        .name("auto-extinguish")
        .description("Automatically extinguishes fire on entities.")
        .defaultValue(false)
        .visible(waterEnabled::get)
        .build()
    );

    private final Setting<Boolean> waterEntityBuckets = sgWater.add(new BoolSetting.Builder()
        .name("entity-buckets")
        .description("Allow usage of entity(Axolotl, Pufferfish, etc.) buckets.")
        .defaultValue(false)
        .visible(waterEnabled::get)
        .build()
    );

    // Lava bucket

    private final Setting<Boolean> lavaEnabled = sgLava.add(new BoolSetting.Builder()
        .name("lava-bucket")
        .description("Whether or not to enable lava-bucket aura.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> lavaEntities = sgLava.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to target.")
        .defaultValue(
            EntityType.CAVE_SPIDER,
            EntityType.CREEPER,
            EntityType.DROWNED,
            EntityType.EVOKER,
            EntityType.HOGLIN,
            EntityType.HUSK,
            EntityType.ILLUSIONER,
            EntityType.PIGLIN_BRUTE,
            EntityType.PILLAGER,
            EntityType.PLAYER,
            EntityType.RAVAGER,
            EntityType.SILVERFISH,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.VINDICATOR,
            EntityType.WITCH,
            EntityType.ZOMBIE
        )
        .filter(entity -> !entity.isFireImmune())
        .visible(lavaEnabled::get)
        .build()
    );

    private final Setting<PlayerTargets> lavaPlayerTargets = sgLava.add(new EnumSetting.Builder<PlayerTargets>()
        .name("player-targets")
        .description("Other players to be targeted.")
        .defaultValue(PlayerTargets.NotFriends)
        .visible(() -> lavaEnabled.get() && lavaEntities.get().getBoolean(EntityType.PLAYER))
        .build()
    );

    private final Setting<Boolean> lavaAntiSuicide = sgLava.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Avoid placing lava on same block as player.")
        .defaultValue(true)
        .visible(lavaEnabled::get)
        .build()
    );

    private final Setting<Boolean> lavaIgnoreOnFire = sgLava.add(new BoolSetting.Builder()
        .name("ignore-on-fire")
        .description("Ignore entities already burning with fire.")
        .defaultValue(true)
        .visible(lavaEnabled::get)
        .build()
    );

    private int placeDelayLeft;
    private BlockPos placePos;
    private FindItemResult placeItem;

    private int collectDelayLeft;

    public BucketAura() {
        super(Categories.Combat, "bucket-aura", "Automatically places and collects buckets.");
    }

    @Override
    public void onActivate() {
        placeDelayLeft = placeDelay.get();
        collectDelayLeft = collectDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (placeDelayLeft <= 0) {
            Entity target = findTarget();
            if (target != null) {
                interact(placePos, placeItem);
                placeDelayLeft = placeDelay.get();
            }
        }

        if (collectDelayLeft <= 0 && collect.get()) {
            BlockIterator.register(range.get(), range.get(), (bp, blockState) -> {
                if (collectDelayLeft > 0) return;
                if (!blockState.getFluidState().isStill()) return;
                if (!canInteractPos(bp.down(), true)) return;

                interact(bp, InvUtils.findInHotbar(Items.BUCKET));
                collectDelayLeft = collectDelay.get();
            });
        }

        placeDelayLeft--;
        collectDelayLeft--;
    }

    private Entity findTarget() {
        for (Entity entity : mc.world.getEntities()) {
            if (!entity.isAlive()) continue;
            if (!entity.getBlockStateAtPos().getFluidState().isEmpty() || entity.inPowderSnow) continue;
            if (ignoreBabies.get() && entity instanceof LivingEntity living && living.isBaby()) continue;

            if (findPlace(entity) && findItem(checkWater(entity), checkLava(entity)))
                return entity;
        }

        return null;
    }

    private boolean findPlace(Entity entity) {
        placePos = null;
        Streams.stream(mc.world.getBlockCollisions(entity, entity.getBoundingBox().offset(0, -0.5, 0)))
            .map(VoxelShape::getBoundingBox)
            .filter(bb -> canInteractPos(new BlockPos(bb.getCenter()), false))
            .findAny()
            .ifPresent(bb -> placePos = new BlockPos(bb.getCenter()));

        return placePos != null;
    }

    private boolean canInteractPos(BlockPos bp, boolean replaceable) {
        Vec3d interactPos = bp.toCenterPos().add(0, 0.5, 0);
        if (!PlayerUtils.isWithin(interactPos, range.get()) || !PlayerUtils.canSeePos(interactPos)) return false;

        BlockState state = mc.world.getBlockState(bp);
        return !state.hasBlockEntity() && (replaceable == state.isReplaceable());
    }

    private boolean checkWater(Entity entity) {
        if (!waterEnabled.get()) return false;
        if (entity instanceof OtherClientPlayerEntity player && !waterPlayerTargets.get().test(player)) return false;

        if ((antiExplode.get() && EntityUtils.canExplode(entity)) ||
            (waterExtinguish.get() && entity.isOnFire()) ||
            (waterEntities.get().getBoolean(entity.getType()) && entity != mc.player && entity != mc.cameraEntity)) return true;

        return false;
    }

    private boolean checkLava(Entity entity) {
        if (!lavaEnabled.get()) return false;
        if (lavaIgnoreOnFire.get() && entity.isOnFire()) return false;
        if (entity == mc.player || entity == mc.cameraEntity) return  false;
        if (lavaAntiSuicide.get() && mc.player.getBlockPos().equals(placePos)) return false;
        if (entity instanceof OtherClientPlayerEntity player && !lavaPlayerTargets.get().test(player)) return false;

        return lavaEntities.get().getBoolean(entity.getType());
    }

    private boolean findItem(boolean checkWater, boolean checkLava) {
        FindItemResult water = new FindItemResult(-1, -1);
        FindItemResult lava = new FindItemResult(-1, -1);
        placeItem = null;

        if (checkWater && !mc.world.getDimension().ultrawarm()) {
            water = InvUtils.findInHotbar(itemStack -> itemStack.isOf(Items.WATER_BUCKET) ||
                        (waterEntityBuckets.get() && itemStack.getItem() instanceof EntityBucketItem));
        }

        if (checkLava) {
            lava = InvUtils.findInHotbar(Items.LAVA_BUCKET);
        }

        placeItem = priority.get().chooseItem(water, lava);

        return placeItem != null && placeItem.found();
    }

    private void interact(BlockPos pos, FindItemResult item) {
        if (!item.found()) return;

        if (smashAdjacent.get()) {
            if (mc.world.getBlockState(pos.up()).getBlock().getHardness() == 0.0)
                mc.interactionManager.attackBlock(pos.up(), Direction.DOWN);

            for (HorizontalDirection direction : HorizontalDirection.values()) {
                BlockPos offsetPos = direction.offset(pos).up();
                if (mc.world.getBlockState(offsetPos).getBlock().getHardness() == 0.0)
                    mc.interactionManager.attackBlock(offsetPos, Direction.DOWN);
            }
        }

        Item interactItem = mc.player.getInventory().getStack(item.slot()).getItem();

        if (interactItem instanceof BucketItem) {
            Vec3d interactPos = pos.toCenterPos().add(0, 0.5, 0);
            Rotations.rotate(Rotations.getYaw(interactPos), Rotations.getPitch(interactPos), 10, () -> {
                InvUtils.swap(item.slot(), true);
                mc.interactionManager.interactItem(mc.player, item.getHand());
                InvUtils.swapBack();
            });
        } else if (interactItem instanceof PowderSnowBucketItem) {
            BlockUtils.place(pos.up(), item, 10, false);
        }
    }

    public enum AuraPriority {
        Water,
        Lava;

        public FindItemResult chooseItem(FindItemResult water, FindItemResult lava) {
            FindItemResult notFound = new FindItemResult(-1, -1);
            return switch (this) {
                case Water -> water.found() ? water : lava.found() ? lava : notFound;
                case Lava -> lava.found() ? lava : water.found() ? water: notFound;
            };
        }
    }

    public enum PlayerTargets {
        All,
        _Friends,
        NotFriends;

        public boolean test(OtherClientPlayerEntity player) {
            return switch (this) {
                case All -> true;
                case _Friends -> Friends.get().isFriend(player);
                case NotFriends -> !Friends.get().isFriend(player);
            };
        }

        @Override
        public String toString() {
            return this == _Friends ? "Friends" : super.toString();
        }
    }
}
