/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import it.unimi.dsi.fastutil.floats.FloatPredicate;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IMultiPlayerGameMode;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class NoFall extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The way you are saved from fall damage.")
        .defaultValue(Mode.Packet)
        .onChanged(value -> {
            clearPlacement();
            if (isActive()) updatePathManager(value);
        })
        .build()
    );

    private final Setting<Boolean> resetOnDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-on-disable")
        .description("Sends reset packets when disabling No Ground.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.NoGround)
        .build()
    );

    private final Setting<PlacedItem> placedItem = sgGeneral.add(new EnumSetting.Builder<PlacedItem>()
        .name("placed-item")
        .description("Which block to place.")
        .defaultValue(PlacedItem.Bucket)
        .visible(() -> mode.get() == Mode.Place)
        .build()
    );

    private final Setting<PlaceMode> airPlaceMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("air-place-mode")
        .description("Whether place mode places before you die or before you take damage.")
        .defaultValue(PlaceMode.BeforeDeath)
        .visible(() -> mode.get() == Mode.AirPlace)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates toward placement and bucket pickup targets.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Place || mode.get() == Mode.AirPlace)
        .build()
    );

    private final Setting<Boolean> anchor = sgGeneral.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Centers the player and reduces movement when using bucket or air place mode.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Place || mode.get() == Mode.AirPlace)
        .build()
    );

    private final Setting<Boolean> antiBounce = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-bounce")
        .description("Disables bouncing on slime-block and bed upon landing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnMace = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-mace")
        .description("Pauses NoFall when using a mace.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.NoGround)
        .build()
    );

    private ClientLevel placementWorld;
    private BlockPos targetPos;
    private PlacedItem recoveryItem;
    private int timer, settledTicks;
    private boolean usingBucket, collecting;
    private Runnable pendingBucketUse;
    private int placementSequence = -1;
    private int pickupSequence = -1;
    private int acknowledgedSequence = -1;
    private boolean prePathManagerNoFall;

    public NoFall() {
        super(Categories.Movement, "no-fall", "Attempts to prevent you from taking fall damage.");
    }

    @Override
    public void onActivate() {
        prePathManagerNoFall = PathManagers.get().getSettings().getNoFall().get();
        updatePathManager(mode.get());
        clearPlacement();
        placementWorld = mc.level;
        acknowledgedSequence = -1;
    }

    @Override
    public void onDeactivate() {
        PathManagers.get().getSettings().getNoFall().set(prePathManagerNoFall);
        clearPlacement();
        if (mode.get() == Mode.NoGround && resetOnDisable.get() && Utils.canUpdate()) {
            sendResetPacket(0.0000008);
            sendResetPacket(0);
        }
    }

    private void updatePathManager(Mode mode) {
        PathManagers.get().getSettings().getNoFall().set(mode == Mode.Packet || mode == Mode.NoGround || prePathManagerNoFall);
    }

    private void sendResetPacket(double height) {
        ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.Pos(
            mc.player.getX(), mc.player.getY() + height, mc.player.getZ(), false, false);
        ((IServerboundMovePlayerPacket) packet).meteor$setTag(1337);
        mc.player.connection.send(packet);
    }

    private void clearPlacement() {
        targetPos = null;
        recoveryItem = null;
        timer = settledTicks = 0;
        placementSequence = -1;
        pickupSequence = -1;
        pendingBucketUse = null;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        clearPlacement();
        placementWorld = null;
        acknowledgedSequence = -1;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundBlockChangedAckPacket packet) {
            mc.execute(() -> {
                if (mc.getConnection() != null && event.connection == mc.getConnection().getConnection()) {
                    acknowledgedSequence = packet.sequence();
                }
            });
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (usingBucket) {
            int sequence = event.packet instanceof ServerboundUseItemPacket packet ? packet.sequence()
                : event.packet instanceof ServerboundUseItemOnPacket packet ? packet.sequence() : -1;
            if (sequence >= 0) {
                if (collecting) pickupSequence = sequence;
                else placementSequence = sequence;
            }
        }

        if (mc.player == null || mc.player.getAbilities().instabuild
            || !(event.packet instanceof ServerboundMovePlayerPacket packet)
            || ((IServerboundMovePlayerPacket) packet).meteor$getTag() == 1337) return;

        if (mode.get() == Mode.NoGround) {
            ((ServerboundMovePlayerPacketAccessor) packet).meteor$setOnGround(false);
            return;
        }

        if (mode.get() != Mode.Packet || pauseOnMace.get() && mc.player.getMainHandItem().getItem() instanceof MaceItem) return;

        if (!Modules.get().isActive(Flight.class)) {
            if (mc.player.isFallFlying()) return;
            if (mc.player.getDeltaMovement().y > -0.5) return;
            ((ServerboundMovePlayerPacketAccessor) event.packet).meteor$setOnGround(true);
        } else {
            ((ServerboundMovePlayerPacketAccessor) event.packet).meteor$setOnGround(true);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (mc.player.getAbilities().instabuild) return;
        if (pauseOnMace.get() && mc.player.getMainHandItem().getItem() instanceof MaceItem) return;

        // Airplace mode
        if (mode.get() == Mode.AirPlace) {
            // Test if fall damage setting is valid
            if (!airPlaceMode.get().test((float) mc.player.fallDistance)) return;

            // Center and place block
            if (anchor.get()) PlayerUtils.centerPlayer();

            Runnable place = () -> {
                double preY = mc.player.getDeltaMovement().y;
                ((IVec3) mc.player.getDeltaMovement()).meteor$setY(0);

                BlockUtils.place(mc.player.blockPosition().below(), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), false, 0, true);

                ((IVec3) mc.player.getDeltaMovement()).meteor$setY(preY);
            };
            if (rotate.get()) Rotations.rotate(mc.player.getYRot(), 90, Integer.MAX_VALUE, place);
            else place.run();
        }
    }

    @EventHandler
    private void onPlaceTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || mode.get() != Mode.Place) return;
        if (placementWorld != mc.level) {
            clearPlacement();
            acknowledgedSequence = -1;
            placementWorld = mc.level;
        }
        if (mc.player.getAbilities().instabuild || mc.player.isSpectator()) return;
        if (pauseOnMace.get() && mc.player.getMainHandItem().getItem() instanceof MaceItem) return;

        if (targetPos != null) {
            timer++;
            boolean settled = mc.player.fallDistance == 0 && (mc.player.onGround()
                || recoveryItem == PlacedItem.PowderSnow && mc.player.isInPowderSnow && mc.player.getDeltaMovement().y >= -0.1);
            // A completed landing remains safe while waiting for the server acknowledgment.
            if (settledTicks < 2) settledTicks = settled ? settledTicks + 1 : 0;

            // Keep the target through pickup prediction so rejected pickups can retry.
            if (pickupSequence >= 0 && acknowledgedSequence < pickupSequence) {
                if (timer > recoveryTimeout(PlayerUtils.getPing())) clearPlacement();
                return;
            }
            pickupSequence = -1;

            if (canCollect(placementSequence, acknowledgedSequence, settledTicks)) {
                boolean exists = recoveryItem == PlacedItem.Bucket
                    ? mc.level.getFluidState(targetPos).isSourceOfType(Fluids.WATER)
                    : mc.level.getBlockState(targetPos).is(Blocks.POWDER_SNOW);
                if (!exists) clearPlacement();
                else {
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.UP, targetPos, false);
                    queueBucketUse(InvUtils.findInHotbar(Items.BUCKET), hit, recoveryItem, true);
                }
            }

            // A rejected prediction must not prevent the next clutch.
            if (targetPos != null && pickupSequence < 0 && placementSequence >= 0 && acknowledgedSequence >= placementSequence
                && !(recoveryItem == PlacedItem.Bucket ? mc.level.getFluidState(targetPos).isSourceOfType(Fluids.WATER)
                    : mc.level.getBlockState(targetPos).is(Blocks.POWDER_SNOW))) clearPlacement();
            if (timer > recoveryTimeout(PlayerUtils.getPing())) clearPlacement();
            if (targetPos != null) return;
        }

        // A rejected water prediction may have reset the client's fall distance.
        double speedY = mc.player.getDeltaMovement().y;
        if (speedY >= 0 || (mc.player.fallDistance <= 3 && speedY > -0.6) || mc.player.isInWater()) return;
        PlacedItem item = mc.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)
            && placedItem.get() == PlacedItem.Bucket ? PlacedItem.PowderSnow : placedItem.get();
        FindItemResult found = InvUtils.findInHotbar(item.item);
        if (!found.found()) return;

        if (anchor.get()) PlayerUtils.centerPlayer();

        BlockHitResult hit = findLandingSurface(mc.player.getYRot());
        if (hit == null) return;

        if (item == PlacedItem.Bucket || item == PlacedItem.PowderSnow) {
            queueBucketUse(found, hit, item, false);
        } else if (isWithinPlaceReach(hit.getLocation())) {
            BlockUtils.place(hit.getBlockPos().above(), found, rotate.get(), 10, true);
        }
    }

    @EventHandler
    private void onSendMovementPackets(SendMovementPacketsEvent.Post event) {
        Runnable use = pendingBucketUse;
        pendingBucketUse = null;
        if (use != null) use.run();
    }

    private static boolean canCollect(int placementSequence, int acknowledgedSequence, int settledTicks) {
        return placementSequence >= 0 && acknowledgedSequence >= placementSequence && settledTicks >= 2;
    }

    private static int recoveryTimeout(int ping) {
        return 20 + (int) Math.ceil(Math.max(0, ping) / 50.0);
    }

    public boolean cancelBounce() {
        return isActive() && antiBounce.get();
    }

    private boolean isWithinPlaceReach(Vec3 pos) {
        // Reach does not extend the server's interaction range.
        double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        return mc.player.getEyePosition().distanceToSqr(pos) <= range * range;
    }

    private BlockHitResult findLandingSurface(float movementYaw) {
        Vec3 pos = mc.player.position();
        Vec3 velocity = mc.player.getDeltaMovement();
        AABB box = mc.player.getBoundingBox();
        double gravity = mc.player.getGravity();
        if (mc.player.hasEffect(MobEffects.SLOW_FALLING)) gravity = Math.min(gravity, 0.01);
        double drag = mc.player.getAttributeValue(Attributes.AIR_DRAG_MODIFIER);
        double horizontalDrag = mc.player.shouldDiscardFriction() ? 1 : Math.clamp(1 - 0.09 * drag, 0, 1);
        double verticalDrag = mc.player.shouldDiscardFriction() ? 1 : Math.clamp(1 - 0.02 * drag, 0, 1);
        Vec3 input = new Vec3(mc.player.xxa, 0, mc.player.zza);
        if (input.lengthSqr() > 1) input = input.normalize();
        input = input.scale(mc.player.isSprinting() ? 0.026 : 0.02);
        double yaw = Math.toRadians(movementYaw);
        Vec3 acceleration = new Vec3(input.x * Math.cos(yaw) - input.z * Math.sin(yaw), 0,
            input.z * Math.cos(yaw) + input.x * Math.sin(yaw));

        // Simulate only the nearby landing window, without changing the player's movement.
        for (int tick = 0; tick < 8; tick++) {
            velocity = velocity.add(acceleration);
            Vec3 movement = Entity.collideBoundingBox(mc.player, velocity, box, mc.level,
                mc.level.getEntityCollisions(mc.player, box.expandTowards(velocity)));
            Vec3 next = pos.add(movement);
            BlockHitResult path = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, mc.player));
            if (path.getType() == HitResult.Type.BLOCK && mc.level.getFluidState(path.getBlockPos()).is(FluidTags.WATER)) return null;

            if (movement.y > velocity.y + 1e-7) {
                // The bounding box can land on a ledge before its center reaches it.
                double radius = mc.player.getBbWidth() / 2.0 - 0.001;
                for (int corner = 0; corner < 5; corner++) {
                    double x = corner == 0 ? 0 : ((corner & 1) == 0 ? radius : -radius);
                    double z = corner == 0 ? 0 : (corner <= 2 ? radius : -radius);
                    Vec3 feet = next.add(x, 0, z);
                    BlockHitResult hit = mc.level.clip(new ClipContext(feet.add(0, 0.01, 0), feet.subtract(0, 0.01, 0),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
                    if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP) return hit;
                }
                return null;
            }

            box = box.move(movement);
            pos = next;
            velocity = new Vec3(movement.x == velocity.x ? velocity.x * horizontalDrag : 0,
                (velocity.y - gravity) * verticalDrag, movement.z == velocity.z ? velocity.z * horizontalDrag : 0);
        }
        return null;
    }

    private void queueBucketUse(FindItemResult item, BlockHitResult hit, PlacedItem placedItem, boolean pickup) {
        if (!item.found()) return;
        ClientLevel world = mc.level;
        float movementYaw = mc.player.getYRot();
        Runnable use = () -> {
            if (!isActive() || !Utils.canUpdate() || mc.level != world || mode.get() != Mode.Place) return;
            if (pauseOnMace.get() && mc.player.getMainHandItem().getItem() instanceof MaceItem) return;
            if (pickup && (targetPos == null || !targetPos.equals(hit.getBlockPos()) || pickupSequence >= 0)) return;

            BlockHitResult landing = pickup ? hit : findLandingSurface(movementYaw);
            if (landing == null) return;

            Vec3 aim = pickup ? Vec3.atCenterOf(hit.getBlockPos()) : landing.getLocation();
            BlockHitResult currentHit = landing;
            if (!pickup) {
                // Recheck placement after movement; pickup uses the normal bucket interaction.
                Vec3 eyes = mc.player.getEyePosition();
                Vec3 end = aim.add(aim.subtract(eyes).normalize().scale(0.01));
                currentHit = mc.level.clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
                if (currentHit.getType() != HitResult.Type.BLOCK || !currentHit.getBlockPos().equals(landing.getBlockPos())
                    || !isWithinPlaceReach(currentHit.getLocation()) || currentHit.getDirection() != Direction.UP || mc.player.onGround()) return;
            }

            if (!useBucket(item, currentHit, placedItem, pickup, pickup ? aim : currentHit.getLocation())) return;
            if (!pickup) {
                targetPos = placedItem == PlacedItem.Bucket && mc.level.getFluidState(currentHit.getBlockPos()).isSourceOfType(Fluids.WATER)
                    ? currentHit.getBlockPos() : currentHit.getBlockPos().above();
                recoveryItem = placedItem;
                timer = settledTicks = 0;
            }
        };

        // Use reachable water before falling again, rather than waiting until after movement.
        boolean useNow = pickup || isWithinPlaceReach(hit.getLocation());
        Vec3 aim = pickup ? Vec3.atCenterOf(hit.getBlockPos()) : hit.getLocation();
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(aim), Rotations.getPitch(aim),
            Integer.MAX_VALUE, true, useNow ? null : use);
        else if (!useNow) pendingBucketUse = use;
        if (useNow) use.run();
    }

    private boolean useBucket(FindItemResult item, BlockHitResult hit, PlacedItem placedItem, boolean pickup, Vec3 aim) {
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        int slot = mc.player.getInventory().getSelectedSlot();
        InteractionHand hand = item.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        try {
            // Bucket use packets carry their own aim without a movement rotation.
            mc.player.setYRot((float) Rotations.getYaw(aim));
            mc.player.setXRot((float) Rotations.getPitch(aim));
            if (hand == InteractionHand.MAIN_HAND) mc.player.getInventory().setSelectedSlot(item.slot());
            if (!mc.player.getItemInHand(hand).is(pickup ? Items.BUCKET : placedItem.item)) return false;
            usingBucket = true;
            collecting = pickup;
            if (pickup) pickupSequence = -1;
            else placementSequence = -1;
            InteractionResult result = !pickup && placedItem == PlacedItem.PowderSnow
                ? mc.gameMode.useItemOn(mc.player, hand, hit) : mc.gameMode.useItem(mc.player, hand);
            return result.consumesAction();
        } finally {
            usingBucket = false;
            collecting = false;
            if (hand == InteractionHand.MAIN_HAND) {
                mc.player.getInventory().setSelectedSlot(slot);
                ((IMultiPlayerGameMode) mc.gameMode).meteor$syncSelected();
            }
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }
    }

    @Override
    public String getInfoString() {
        return mode.get().toString();
    }

    public enum Mode {
        Packet,
        NoGround,
        AirPlace,
        Place
    }

    public enum PlacedItem {
        Bucket(Items.WATER_BUCKET),
        PowderSnow(Items.POWDER_SNOW_BUCKET),
        HayBale(Items.HAY_BLOCK),
        Cobweb(Items.COBWEB),
        SlimeBlock(Items.SLIME_BLOCK);

        private final Item item;

        PlacedItem(Item item) {
            this.item = item;
        }
    }

    public enum PlaceMode {
        BeforeDamage(height -> height > 2),
        BeforeDeath(height -> height > Math.max(PlayerUtils.getTotalHealth(), 2));

        private final FloatPredicate fallHeight;

        PlaceMode(FloatPredicate fallHeight) {
            this.fallHeight = fallHeight;
        }

        public boolean test(float fallHeight) {
            return this.fallHeight.test(fallHeight);
        }
    }
}
