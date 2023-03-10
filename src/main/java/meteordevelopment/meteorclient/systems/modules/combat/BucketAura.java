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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class BucketAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWater = settings.createGroup("Water");
    private final SettingGroup sgLava = settings.createGroup("Lava");

    private final Setting<AuraPriority> auraPriority = sgGeneral.add(new EnumSetting.Builder<AuraPriority>()
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
        .defaultValue(5)
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
        .defaultValue(2)
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

    private final Setting<Boolean> antiExplode = sgGeneral.add(new BoolSetting.Builder() // add me to lava
        .name("anti-explode")
        .description("Automatically places water/lava to prevent block damage from explosions.")
        .defaultValue(true)
        .visible(() -> !collect.get())
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
        .defaultValue(PlayerTargets.OnlyFriends)
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
    private Vec3d placePos;
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
                if (!canInteractPos(bp.toCenterPos(), false)) return;

                interact(bp.toCenterPos(), InvUtils.findInHotbar(Items.BUCKET));
                collectDelayLeft = collectDelay.get();
            });
        }

        placeDelayLeft--;
        collectDelayLeft--;
    }

    private Entity findTarget() {
        for (Entity entity : mc.world.getEntities()) {
            if (!entity.isAlive()) continue;
            if (entity.isTouchingWater() || entity.isInLava() || entity.inPowderSnow) continue;
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
            .filter(bb -> canInteractPos(bb.getCenter().add(0, 0.5, 0), true))
            .findAny()
            .ifPresent(bb -> placePos = bb.getCenter().add(0, 0.5, 0));

        return placePos != null;
    }

    private boolean canInteractPos(Vec3d pos, boolean place) {
        if (!PlayerUtils.isWithin(pos, range.get()) || !PlayerUtils.canSeePos(pos)) return false;

        if (place) {
            BlockState belowState = mc.world.getBlockState(new BlockPos(pos).down());
            return !belowState.hasBlockEntity() && !belowState.isReplaceable();
        }

        return true;
    }

    private boolean checkWater(Entity entity) {
        if (!waterEnabled.get()) return false;
        if (entity instanceof OtherClientPlayerEntity player && !waterPlayerTargets.get().test(player)) return false;

        if ((antiExplode.get() && !collect.get() && EntityUtils.canExplode(entity)) ||
            (waterExtinguish.get() && entity.isOnFire()) ||
            (waterEntities.get().getBoolean(entity.getType()) && entity != mc.player && entity != mc.cameraEntity)) return true;

        return false;
    }

    private boolean checkLava(Entity entity) {
        if (!lavaEnabled.get()) return false;
        if (lavaIgnoreOnFire.get() && entity.isOnFire()) return false;
        if (entity == mc.player || entity == mc.cameraEntity) return false;
        if (lavaAntiSuicide.get() && mc.player.getBlockPos().equals(placePos)) return false;
        if (entity instanceof OtherClientPlayerEntity player && !lavaPlayerTargets.get().test(player)) return false;

        if ((antiExplode.get() && !collect.get() && EntityUtils.canExplode(entity)) ||
            (lavaEntities.get().getBoolean(entity.getType()))) return true;

        return false;
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

        placeItem = auraPriority.get().chooseItem(water, lava);

        return placeItem != null && placeItem.found();
    }

    private void interact(Vec3d pos, FindItemResult item) {
        if (!item.found()) return;

        BlockPos blockPos = new BlockPos(pos);

        if (smashAdjacent.get()) {
            if (mc.world.getBlockState(blockPos).getBlock().getHardness() == 0.0)
                mc.interactionManager.attackBlock(blockPos, Direction.DOWN);

            for (HorizontalDirection direction : HorizontalDirection.values()) {
                BlockPos offsetPos = direction.offset(blockPos);
                if (mc.world.getBlockState(offsetPos).getBlock().getHardness() == 0.0)
                    mc.interactionManager.attackBlock(offsetPos, Direction.DOWN);
            }
        }

        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 10, () -> {
            InvUtils.swap(item.slot(), true);
            mc.interactionManager.interactItem(mc.player, item.getHand());
            InvUtils.swapBack();
        });
    }

    public enum AuraPriority {
        Water,
        Lava;

        public FindItemResult chooseItem(FindItemResult water, FindItemResult lava) {
            return switch (this) {
                case Water -> water.found() ? water : lava;
                case Lava -> lava.found() ? lava : water;
            };
        }
    }

    public enum PlayerTargets {
        All,
        OnlyFriends,
        NotFriends;

        public boolean test(OtherClientPlayerEntity player) {
            return switch (this) {
                case All -> true;
                case OnlyFriends -> Friends.get().isFriend(player);
                case NotFriends -> !Friends.get().isFriend(player);
            };
        }
    }
}
