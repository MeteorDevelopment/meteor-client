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
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
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
    private boolean usingBucket;
    private Runnable pendingBucketUse;
    private int placementSequence = -1;
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
            if (event.packet instanceof ServerboundUseItemPacket packet) placementSequence = packet.getSequence();
            else if (event.packet instanceof ServerboundUseItemOnPacket packet) placementSequence = packet.getSequence();
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
            settledTicks = settled ? settledTicks + 1 : 0;

            if (canCollect(placementSequence, acknowledgedSequence, settledTicks)) {
                boolean exists = recoveryItem == PlacedItem.Bucket
                    ? mc.level.getFluidState(targetPos).isSourceOfType(Fluids.WATER)
                    : mc.level.getBlockState(targetPos).is(Blocks.POWDER_SNOW);
                if (!exists) clearPlacement();
                else {
                    Vec3 eyes = mc.player.getEyePosition();
                    BlockHitResult hit = mc.level.clip(new ClipContext(eyes, Vec3.atCenterOf(targetPos),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, mc.player));
                    if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(targetPos)
                        && isWithinBucketReach(hit.getLocation())) queueBucketUse(InvUtils.findInHotbar(Items.BUCKET), hit, recoveryItem, true);
                }
            }

            // A rejected prediction must not prevent the next clutch.
            if (targetPos != null && placementSequence >= 0 && acknowledgedSequence >= placementSequence
                && !(recoveryItem == PlacedItem.Bucket ? mc.level.getFluidState(targetPos).isSourceOfType(Fluids.WATER)
                    : mc.level.getBlockState(targetPos).is(Blocks.POWDER_SNOW))) clearPlacement();
            if (timer > recoveryTimeout(PlayerUtils.getPing())) clearPlacement();
            if (targetPos != null) return;
        }

        // A rejected water prediction may have reset the client's fall distance.
        double speedY = mc.player.getDeltaMovement().y;
        if (speedY >= 0 || (mc.player.fallDistance <= 3 && speedY > -0.6) || EntityUtils.isAboveWater(mc.player)) return;
        PlacedItem item = mc.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)
            && placedItem.get() == PlacedItem.Bucket ? PlacedItem.PowderSnow : placedItem.get();
        FindItemResult found = InvUtils.findInHotbar(item.item);
        if (!found.found()) return;

        if (anchor.get()) PlayerUtils.centerPlayer();

        // Find the landing surface before this tick's falling movement.
        Vec3 feet = mc.player.position();
        BlockHitResult hit = mc.level.clip(new ClipContext(feet, feet.subtract(0, 5, 0),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return;

        if (item == PlacedItem.Bucket || item == PlacedItem.PowderSnow) {
            queueBucketUse(found, hit, item, false);
        } else {
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

    private boolean isWithinBucketReach(Vec3 pos) {
        // Reach only extends the client ray, not the server's bucket reach.
        double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        return mc.player.getEyePosition().distanceToSqr(pos) <= range * range;
    }

    private void queueBucketUse(FindItemResult item, BlockHitResult hit, PlacedItem placedItem, boolean pickup) {
        if (!item.found()) return;
        ClientLevel world = mc.level;
        Runnable use = () -> {
            if (!isActive() || !Utils.canUpdate() || mc.level != world || mode.get() != Mode.Place) return;
            if (pauseOnMace.get() && mc.player.getMainHandItem().getItem() instanceof MaceItem) return;

            // Recheck the ray after movement, before using the bucket.
            Vec3 eyes = mc.player.getEyePosition();
            Vec3 end = pickup ? Vec3.atCenterOf(hit.getBlockPos())
                : eyes.subtract(0, mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), 0);
            BlockHitResult currentHit = mc.level.clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE,
                pickup ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE, mc.player));
            if (currentHit.getType() != HitResult.Type.BLOCK || !currentHit.getBlockPos().equals(hit.getBlockPos())
                || !isWithinBucketReach(currentHit.getLocation())) return;
            if (!pickup && (currentHit.getDirection() != Direction.UP || mc.player.onGround())) return;

            if (!useBucket(item, currentHit, placedItem, pickup)) return;
            if (pickup) clearPlacement();
            else {
                targetPos = placedItem == PlacedItem.Bucket && mc.level.getFluidState(currentHit.getBlockPos()).isSourceOfType(Fluids.WATER)
                    ? currentHit.getBlockPos() : currentHit.getBlockPos().above();
                recoveryItem = placedItem;
                timer = settledTicks = 0;
            }
        };

        // Use reachable water before falling again, rather than waiting until after movement.
        boolean useNow = isWithinBucketReach(hit.getLocation());
        if (rotate.get()) Rotations.rotate(Rotations.getYaw(hit.getLocation()), Rotations.getPitch(hit.getLocation()),
            Integer.MAX_VALUE, true, useNow ? null : use);
        else if (!useNow) pendingBucketUse = use;
        if (useNow) use.run();
    }

    private boolean useBucket(FindItemResult item, BlockHitResult hit, PlacedItem placedItem, boolean pickup) {
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        int slot = mc.player.getInventory().getSelectedSlot();
        InteractionHand hand = item.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        try {
            // Bucket use packets carry their own aim without a movement rotation.
            mc.player.setYRot((float) Rotations.getYaw(hit.getLocation()));
            mc.player.setXRot((float) Rotations.getPitch(hit.getLocation()));
            if (hand == InteractionHand.MAIN_HAND) mc.player.getInventory().setSelectedSlot(item.slot());
            if (!mc.player.getItemInHand(hand).is(pickup ? Items.BUCKET : placedItem.item)) return false;
            usingBucket = !pickup;
            if (usingBucket) placementSequence = -1;
            InteractionResult result = !pickup && placedItem == PlacedItem.PowderSnow
                ? mc.gameMode.useItemOn(mc.player, hand, hit) : mc.gameMode.useItem(mc.player, hand);
            return result.consumesAction();
        } finally {
            usingBucket = false;
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
