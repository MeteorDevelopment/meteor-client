/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.StrongholdLocator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class LocateCommand extends Command {
    private static final int EYE_TRAJECTORY_SAMPLE_TICKS = 80;
    private static final int MIN_EYE_TRAJECTORY_TICKS = 20;

    private boolean awaitingEyeUse;
    private boolean awaitingEyeSpawn;
    private boolean trackingEye;
    private int trackedEyeEntityId = -1;
    private int eyeTrackingTicks;
    private boolean eyeBearingReady;
    private Vec3 firstEyePosition;
    private Vec3 secondEyePosition;
    private Vec3 pendingEyePosition;

    private final List<Block> netherFortressBlocks = List.of(
        Blocks.NETHER_BRICKS,
        Blocks.NETHER_BRICK_FENCE,
        Blocks.NETHER_WART
    );

    private final List<Block> monumentBlocks = List.of(
        Blocks.PRISMARINE_BRICKS,
        Blocks.SEA_LANTERN,
        Blocks.DARK_PRISMARINE
    );

    private final List<Block> strongholdBlocks = List.of(
        Blocks.END_PORTAL_FRAME
    );

    private final List<Block> endCityBlocks = List.of(
        Blocks.PURPUR_BLOCK,
        Blocks.PURPUR_PILLAR,
        Blocks.PURPUR_SLAB,
        Blocks.PURPUR_STAIRS,
        Blocks.END_STONE_BRICKS,
        Blocks.END_ROD
    );

    public LocateCommand() {
        super("locate", "Locates structures", "loc");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        // Overworld structures

        builder.then(literal("buried_treasure").executes(_ -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();
            if (stack.getItem() != Items.FILLED_MAP
                || stack.get(DataComponents.ITEM_NAME) == null
                || !stack.get(DataComponents.ITEM_NAME).getString().equals(Component.translatable("item.minecraft.buried_treasure_map").getString())) {
                error("You need to hold a (highlight)buried treasure map(default)!");
                return SINGLE_SUCCESS;
            }

            MapDecorations mapDecorationsComponent = stack.get(DataComponents.MAP_DECORATIONS);
            if (mapDecorationsComponent == null) {
                error("Couldn't locate the map icons!");
                return SINGLE_SUCCESS;
            }

            for (MapDecorations.Entry decoration : mapDecorationsComponent.decorations().values()) {
                if (decoration.type().value().assetId().toString().equals("minecraft:red_x")) {
                    Vec3 coords = new Vec3(decoration.x(), 62, decoration.z());
                    MutableComponent text = Component.literal("Buried Treasure located at ");
                    text.append(ChatUtils.formatCoords(coords));
                    text.append(".");
                    info(text);
                    return SINGLE_SUCCESS;
                }
            }

            error("Couldn't locate the buried treasure!");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("mansion").executes(_ -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();
            if (stack.getItem() != Items.FILLED_MAP
                || stack.get(DataComponents.ITEM_NAME) == null
                || !stack.get(DataComponents.ITEM_NAME).getString().equals(Component.translatable("item.minecraft.woodland_mansion_map").getString())) {
                error("You need to hold a (highlight)woodland mansion map(default)!");
                return SINGLE_SUCCESS;
            }

            MapDecorations mapDecorationsComponent = stack.get(DataComponents.MAP_DECORATIONS);
            if (mapDecorationsComponent == null) {
                error("Couldn't locate the map icons!");
                return SINGLE_SUCCESS;
            }

            for (MapDecorations.Entry decoration : mapDecorationsComponent.decorations().values()) {
                if (decoration.type().value().assetId().toString().equals("minecraft:woodland_mansion")) {
                    Vec3 coords = new Vec3(decoration.x(), 62, decoration.z());
                    MutableComponent text = Component.literal("Mansion located at ");
                    text.append(ChatUtils.formatCoords(coords));
                    text.append(".");
                    info(text);
                    return SINGLE_SUCCESS;
                }
            }

            error("Couldn't locate the mansion!");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("monument").executes(_ -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();
            if (stack.getItem() == Items.FILLED_MAP
                && stack.get(DataComponents.ITEM_NAME) != null
                && stack.get(DataComponents.ITEM_NAME).getString().equals(Component.translatable("item.minecraft.ocean_monument_map").getString())) {

                MapDecorations mapDecorationsComponent = stack.get(DataComponents.MAP_DECORATIONS);
                if (mapDecorationsComponent == null) {
                    error("Couldn't locate the map icons!");
                    return SINGLE_SUCCESS;
                }

                for (MapDecorations.Entry decoration : mapDecorationsComponent.decorations().values()) {
                    if (decoration.type().value().assetId().toString().equals("minecraft:ocean_monument")) {
                        Vec3 coords = new Vec3(decoration.x(), 62, decoration.z());
                        MutableComponent text = Component.literal("Monument located at ");
                        text.append(ChatUtils.formatCoords(coords));
                        text.append(".");
                        info(text);
                        return SINGLE_SUCCESS;
                    }
                }

                error("Couldn't locate the monument!");
                return SINGLE_SUCCESS;
            }

            // If the player is not holding a valid map, try to locate the monument using Baritone
            if (BaritoneUtils.IS_AVAILABLE) {
                Vec3 coords = findByBlockList(monumentBlocks);
                if (coords == null) {
                    error("No monument found. Try using an (highlight)ocean monument map(default) for more success.");
                    return SINGLE_SUCCESS;
                }
                MutableComponent text = Component.literal("Monument located at ");
                text.append(ChatUtils.formatCoords(coords));
                text.append(".");
                info(text);
                return SINGLE_SUCCESS;
            }

            error("Locating this structure without an (highlight)ocean monument map(default) requires Baritone.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stronghold").executes(_ -> {
            boolean foundEye = InvUtils.testInHotbar(Items.ENDER_EYE);

            if (foundEye) {
                resetTracking();
                awaitingEyeUse = true;
                MeteorClient.EVENT_BUS.subscribe(this);
                info("Throw one Eye of Ender. Its flight path will be sampled for about 4 seconds.");
            } else if (BaritoneUtils.IS_AVAILABLE) {
                Vec3 coords = findByBlockList(strongholdBlocks);
                if (coords == null) {
                    error("No stronghold found nearby. You can use (highlight)Ender Eyes(default) for more success.");
                    return SINGLE_SUCCESS;
                }
                MutableComponent text = Component.literal("Stronghold located at ");
                text.append(ChatUtils.formatCoords(coords));
                text.append(".");
                info(text);
            } else {
                error("No Eyes of Ender found in hotbar.");
            }

            return SINGLE_SUCCESS;
        }));

        // Nether structures

        builder.then(literal("nether_fortress").executes(_ -> {
            if (mc.level.dimension() != Level.NETHER) {
                error("You need to be in the nether to locate a nether fortress.");
                return SINGLE_SUCCESS;
            }

            if (!BaritoneUtils.IS_AVAILABLE) {
                error("Locating this structure requires Baritone.");
                return SINGLE_SUCCESS;
            }

            Vec3 coords = findByBlockList(netherFortressBlocks);
            if (coords == null) {
                error("No nether fortress found.");
                return SINGLE_SUCCESS;
            }
            MutableComponent text = Component.literal("Fortress located at ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        // End structures

        builder.then(literal("end_city").executes(_ -> {
            if (mc.level.dimension() != Level.END) {
                error("You need to be in the end to locate an end city.");
                return SINGLE_SUCCESS;
            }

            if (!BaritoneUtils.IS_AVAILABLE) {
                error("Locating this structure requires Baritone.");
                return SINGLE_SUCCESS;
            }

            Vec3 coords = findByBlockList(endCityBlocks);
            if (coords == null) {
                error("No end city found.");
                return SINGLE_SUCCESS;
            }
            MutableComponent text = Component.literal("End city located at ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        // Misc structures

        builder.then(literal("lodestone").executes(_ -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();
            if (stack.getItem() != Items.COMPASS) {
                error("You need to hold a (highlight)lodestone(default) compass!");
                return SINGLE_SUCCESS;
            }
            DataComponentMap components = stack.getComponents();
            if (components == null) {
                error("Couldn't get the components data. Are you holding a (highlight)lodestone(default) compass?");
                return SINGLE_SUCCESS;
            }
            LodestoneTracker lodestoneTrackerComponent = components.get(DataComponents.LODESTONE_TRACKER);
            if (lodestoneTrackerComponent == null) {
                error("Couldn't get the components data. Are you holding a (highlight)lodestone(default) compass?");
                return SINGLE_SUCCESS;
            }

            if (lodestoneTrackerComponent.target().isEmpty()) {
                error("Couldn't get the lodestone's target!");
                return SINGLE_SUCCESS;
            }

            Vec3 coords = Vec3.atLowerCornerOf(lodestoneTrackerComponent.target().get().pos());
            MutableComponent text = Component.literal("Lodestone located at ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("cancel").executes(_ -> {
            cancel();
            return SINGLE_SUCCESS;
        }));
    }

    private void cancel() {
        warning("Locate canceled");
        resetTracking();
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    // Reset the Eye tracking state before another lookup.
    private void resetTracking() {
        awaitingEyeUse = false;
        awaitingEyeSpawn = false;
        trackingEye = false;
        trackedEyeEntityId = -1;
        eyeTrackingTicks = 0;
        eyeBearingReady = false;
        firstEyePosition = null;
        secondEyePosition = null;
        pendingEyePosition = null;
    }

    private @Nullable Vec3 findByBlockList(List<Block> blockList) {
        List<BlockPos> posList = BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext(), blockList, 64, 10, 32);
        if (posList.isEmpty()) {
            return null;
        }
        if (posList.size() < 3) {
            warning("Only %d block(s) found. This search might be a false positive.", posList.size());
        }
        return new Vec3(posList.getFirst().getX(), posList.getFirst().getY(), posList.getFirst().getZ());
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        resetTracking();
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    // Save the throw origin when the use packet is sent.
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!awaitingEyeUse || mc.player == null) return;

        if (event.packet instanceof ServerboundUseItemPacket && mc.player.getMainHandItem().is(Items.ENDER_EYE)) {
            pendingEyePosition = mc.player.position();
            awaitingEyeSpawn = true;
        }
    }

    // Ignore Eyes that did not come from the pending throw.
    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (!awaitingEyeSpawn || pendingEyePosition == null) return;

        if (event.packet instanceof ClientboundAddEntityPacket packet && packet.getType() == EntityTypes.EYE_OF_ENDER) {
            double dx = packet.getX() - pendingEyePosition.x;
            double dz = packet.getZ() - pendingEyePosition.z;

            if (dx * dx + dz * dz > 16.0) return;

            awaitingEyeUse = false;
            awaitingEyeSpawn = false;
            trackingEye = true;
            trackedEyeEntityId = packet.getId();
            firstEyePosition = new Vec3(packet.getX(), packet.getY(), packet.getZ());
            eyeTrackingTicks = 0;
            eyeBearingReady = false;
            info("Eye detected. Measuring its trajectory (about 4 seconds).");
        }
    }

    // Sample the Eye after it has travelled far enough.
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!trackingEye || mc.level == null || eyeBearingReady) return;
        if (!(mc.level.getEntity(trackedEyeEntityId) instanceof EyeOfEnder eye)) return;

        eyeTrackingTicks++;
        if (eyeTrackingTicks >= EYE_TRAJECTORY_SAMPLE_TICKS) {
            captureEyeBearing(eye.position());
            if (eyeBearingReady) {
                info("Eye trajectory sampled. Waiting for it to disappear...");
            }
        }
    }

    // Reject samples that do not provide a stable direction.
    private void captureEyeBearing(Vec3 secondEyePosition) {
        if (firstEyePosition == null) return;

        double dx = secondEyePosition.x - firstEyePosition.x;
        double dz = secondEyePosition.z - firstEyePosition.z;
        if (!Double.isFinite(dx) || !Double.isFinite(dz) || dx * dx + dz * dz < 4.0) return;

        this.secondEyePosition = secondEyePosition;
        eyeBearingReady = true;
    }

    // Use the last position when the Eye disappears before the scheduled sample.
    @EventHandler
    private void onRemoveEntity(EntityRemovedEvent event) {
        if (!trackingEye || !(event.entity instanceof EyeOfEnder eye) || eye.getId() != trackedEyeEntityId) return;

        if (!eyeBearingReady && eyeTrackingTicks >= MIN_EYE_TRAJECTORY_TICKS) {
            captureEyeBearing(eye.position());
        }

        trackingEye = false;
        MeteorClient.EVENT_BUS.unsubscribe(this);

        if (mc.player == null) {
            resetTracking();
            return;
        }

        if (!eyeBearingReady) {
            resetTracking();
            error("The Eye disappeared before enough of its flight path was measured. Try again.");
            return;
        }

        Vec3 eyeStart = firstEyePosition;
        Vec3 eyeEnd = secondEyePosition;
        resetTracking();

        if (eyeStart == null || eyeEnd == null) {
            error("Could not read the Eye's flight path. Try throwing another Eye.");
            return;
        }

        info("Eye path: %.1f / %.1f -> %.1f / %.1f", eyeStart.x, eyeStart.z, eyeEnd.x, eyeEnd.z);

        // Match the measured path against the valid stronghold rings.
        List<StrongholdLocator.TrajectoryPrediction> predictions = StrongholdLocator.predictFromTrajectory(
            eyeStart.x,
            eyeStart.z,
            eyeEnd.x,
            eyeEnd.z,
            5
        );

        if (predictions.isEmpty()) {
            error("No stronghold candidate matched the Eye's trajectory and ring. Try another throw.");
            return;
        }

        StrongholdLocator.TrajectoryPrediction best = predictions.getFirst();
        MutableComponent text = Component.literal("Best trajectory match: ");
        text.append(ChatUtils.formatCoords(new Vec3(best.blockX(), 0, best.blockZ())));
        text.append(Component.literal(" (grid fit: " + formatAccuracy(best.accuracy()) + ")"));
        info(text);

        for (int i = 1; i < predictions.size(); i++) {
            StrongholdLocator.TrajectoryPrediction alternative = predictions.get(i);
            info("Alternative %d: %d / %d (grid fit: %s)",
                i + 1,
                alternative.blockX(),
                alternative.blockZ(),
                formatAccuracy(alternative.accuracy())
            );
        }
    }

    private static String formatAccuracy(long accuracy) {
        return accuracy == Long.MAX_VALUE ? "exact" : Long.toString(accuracy);
    }
}
