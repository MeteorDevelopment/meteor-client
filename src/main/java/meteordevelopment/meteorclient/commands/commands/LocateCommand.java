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
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.misc.text.MessageBuilder;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.MapDecorationsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LocateCommand extends Command {
    private Vec3d firstStart, firstEnd;
    private Vec3d secondStart, secondEnd;

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
        super("locate", "loc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // Overworld structures

        builder.then(literal("buried_treasure").executes(s -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();
            if (stack.getItem() != Items.FILLED_MAP
                || stack.get(DataComponentTypes.ITEM_NAME) == null
                || !stack.get(DataComponentTypes.ITEM_NAME).getString().equals(Text.translatable("filled_map.buried_treasure").getString())) {
                this.error("no_buried_treasure_map", MessageBuilder.highlight(Text.translatable("filled_map.buried_treasure"))).send();
                return SINGLE_SUCCESS;
            }

            MapDecorationsComponent mapDecorationsComponent = stack.get(DataComponentTypes.MAP_DECORATIONS);
            if (mapDecorationsComponent == null) {
                this.error("no_map_icons").send();
                return SINGLE_SUCCESS;
            }

            for (MapDecorationsComponent.Decoration decoration : mapDecorationsComponent.decorations().values()) {
                if (decoration.type().value().assetId().toString().equals("minecraft:red_x")) {
                    Vec3d coords = new Vec3d(decoration.x(), 62, decoration.z());
                    this.info("buried_treasure", coords).send();
                    return SINGLE_SUCCESS;
                }
            }

            this.error("cant_locate_buried_treasure").send();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("mansion").executes(s -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();
            if (stack.getItem() != Items.FILLED_MAP
                || stack.get(DataComponentTypes.ITEM_NAME) == null
                || !stack.get(DataComponentTypes.ITEM_NAME).getString().equals(Text.translatable("filled_map.mansion").getString())) {
                this.error("no_woodland_explorer_map", MessageBuilder.highlight(Text.translatable("filled_map.mansion"))).send();
                return SINGLE_SUCCESS;
            }

            MapDecorationsComponent mapDecorationsComponent = stack.get(DataComponentTypes.MAP_DECORATIONS);
            if (mapDecorationsComponent == null) {
                this.error("no_map_icons").send();
                return SINGLE_SUCCESS;
            }

            for (MapDecorationsComponent.Decoration decoration : mapDecorationsComponent.decorations().values()) {
                if (decoration.type().value().assetId().toString().equals("minecraft:woodland_mansion")) {
                    Vec3d coords = new Vec3d(decoration.x(), 62, decoration.z());
                    this.info("mansion", coords).send();
                    return SINGLE_SUCCESS;
                }
            }

            this.error("cant_locate_mansion").send();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("monument").executes(s -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();
            if (stack.getItem() == Items.FILLED_MAP
                && stack.get(DataComponentTypes.ITEM_NAME) != null
                && stack.get(DataComponentTypes.ITEM_NAME).getString().equals(Text.translatable("filled_map.monument").getString())) {

                MapDecorationsComponent mapDecorationsComponent = stack.get(DataComponentTypes.MAP_DECORATIONS);
                if (mapDecorationsComponent == null) {
                    this.error("no_map_icons").send();
                    return SINGLE_SUCCESS;
                }

                for (MapDecorationsComponent.Decoration decoration : mapDecorationsComponent.decorations().values()) {
                    if (decoration.type().value().assetId().toString().equals("minecraft:ocean_monument")) {
                        Vec3d coords = new Vec3d(decoration.x(), 62, decoration.z());
                        this.info("monument", coords).send();
                        return SINGLE_SUCCESS;
                    }
                }

                this.error("cant_locate_monument").send();
                return SINGLE_SUCCESS;
            }

            // If the player is not holding a valid map, try to locate the monument using Baritone
            if (BaritoneUtils.IS_AVAILABLE) {
                Vec3d coords = findByBlockList(monumentBlocks);
                if (coords == null) {
                    this.error("no_monument_found", MessageBuilder.highlight(Text.translatable("filled_map.monument"))).send();
                    return SINGLE_SUCCESS;
                }
                this.info("monument", coords).send();
                return SINGLE_SUCCESS;
            }

            this.error("ocean_explorer_no_baritone", MessageBuilder.highlight(Text.translatable("filled_map.monument"))).send();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stronghold").executes(s -> {
            boolean foundEye = InvUtils.testInHotbar(Items.ENDER_EYE);

            if (foundEye) {
                if (BaritoneUtils.IS_AVAILABLE) PathManagers.get().follow(EyeOfEnderEntity.class::isInstance);
                firstStart = null;
                firstEnd = null;
                secondStart = null;
                secondEnd = null;
                MeteorClient.EVENT_BUS.subscribe(this);
                this.info("first_eye").send();
            } else if (BaritoneUtils.IS_AVAILABLE) {
                Vec3d coords = findByBlockList(strongholdBlocks);
                if (coords == null) {
                    this.error("no_stronghold_found", MessageBuilder.highlight(Text.translatable("item.minecraft.ender_eye"))).send();
                    return SINGLE_SUCCESS;
                }
                this.info("stronghold", coords).send();
            } else {
                this.error("no_eyes_of_ender").send();
            }

            return SINGLE_SUCCESS;
        }));

        // Nether structures

        builder.then(literal("nether_fortress").executes(s -> {
            if (mc.world.getRegistryKey() != World.NETHER) {
                this.error("not_in_nether").send();
                return SINGLE_SUCCESS;
            }

            if (!BaritoneUtils.IS_AVAILABLE) {
                this.error("no_baritone").send();
                return SINGLE_SUCCESS;
            }

            Vec3d coords = findByBlockList(netherFortressBlocks);
            if (coords == null) {
                this.error("cant_locate_nether_fortress").send();
                return SINGLE_SUCCESS;
            }

            this.info("nether_fortress", coords).send();
            return SINGLE_SUCCESS;
        }));

        // End structures

        builder.then(literal("end_city").executes(s -> {
            if (mc.world.getRegistryKey() != World.END) {
                this.error("not_in_end").send();
                return SINGLE_SUCCESS;
            }

            if (!BaritoneUtils.IS_AVAILABLE) {
                this.error("no_baritone").send();
                return SINGLE_SUCCESS;
            }

            Vec3d coords = findByBlockList(endCityBlocks);
            if (coords == null) {
                this.error("cant_locate_end_city").send();
                return SINGLE_SUCCESS;
            }

            this.info("end_city", coords).send();
            return SINGLE_SUCCESS;
        }));

        // Misc structures

        builder.then(literal("lodestone").executes(s -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();
            if (stack.getItem() != Items.COMPASS) {
                this.error("no_lodestone_compass", MessageBuilder.highlight(Text.translatable("item.minecraft.lodestone_compass"))).send();
                return SINGLE_SUCCESS;
            }
            ComponentMap components = stack.getComponents();
            if (components == null) {
                this.error("no_lodestone_compass_data", MessageBuilder.highlight(Text.translatable("item.minecraft.lodestone_compass"))).send();
                return SINGLE_SUCCESS;
            }
            LodestoneTrackerComponent lodestoneTrackerComponent = components.get(DataComponentTypes.LODESTONE_TRACKER);
            if (lodestoneTrackerComponent == null) {
                this.error("no_lodestone_compass_data", MessageBuilder.highlight(Text.translatable("item.minecraft.lodestone_compass"))).send();
                return SINGLE_SUCCESS;
            }

            if (lodestoneTrackerComponent.target().isEmpty()) {
                this.error("no_lodestone").send();
                return SINGLE_SUCCESS;
            }

            Vec3d coords = Vec3d.of(lodestoneTrackerComponent.target().get().pos());

            this.info("lodestone", coords).send();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("cancel").executes(s -> {
            cancel();
            return SINGLE_SUCCESS;
        }));
    }

    private void cancel() {
        this.warning("canceled").send();
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    private @Nullable Vec3d findByBlockList(List<Block> blockList) {
        List<BlockPos> posList = BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext(), blockList, 64, 10, 32);
        if (posList.isEmpty()) {
            return null;
        }
        if (posList.size() < 3) {
            this.warning("false_positive", posList.size()).send();
        }
        return new Vec3d(posList.getFirst().getX(), posList.getFirst().getY(), posList.getFirst().getZ());
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySpawnS2CPacket packet && packet.getEntityType() == EntityType.EYE_OF_ENDER) {
            firstPosition(packet.getX(), packet.getY(), packet.getZ());
        }
    }

    @EventHandler
    private void onRemoveEntity(EntityRemovedEvent event) {
        if (event.entity instanceof EyeOfEnderEntity eye) {
            lastPosition(eye.getX(), eye.getY(), eye.getZ());
        }
    }

    private void firstPosition(double x, double y, double z) {
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstStart == null) {
            this.firstStart = pos;
        } else {
            this.secondStart = pos;
        }
    }

    private void lastPosition(double x, double y, double z) {
        this.info(this.firstEnd == null ? "first_eye_saved" : "second_eye_saved").send();
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstEnd == null) {
            this.firstEnd = pos;
            this.info("eye_different_location").send();
        } else {
            this.secondEnd = pos;
            findStronghold();
        }
    }

    private void findStronghold() {
        PathManagers.get().stop();

        if (this.firstStart == null || this.firstEnd == null || this.secondStart == null || this.secondEnd == null) {
            this.error("missing_position_data").send();
            cancel();
            return;
        }

        final double[] start = new double[]{this.secondStart.x, this.secondStart.z, this.secondEnd.x, this.secondEnd.z};
        final double[] end = new double[]{this.firstStart.x, this.firstStart.z, this.firstEnd.x, this.firstEnd.z};
        final double[] intersection = calcIntersection(start, end);
        if (Double.isNaN(intersection[0]) || Double.isNaN(intersection[1]) || Double.isInfinite(intersection[0]) || Double.isInfinite(intersection[1])) {
            this.error("unable_to_calculate").send();
            cancel();
            return;
        }

        MeteorClient.EVENT_BUS.unsubscribe(this);
        Vec3d coords = new Vec3d(intersection[0], 0, intersection[1]);

        this.info("stronghold", coords).send();
    }

    private double[] calcIntersection(double[] line, double[] line2) {
        final double a1 = line[3] - line[1];
        final double b1 = line[0] - line[2];
        final double c1 = a1 * line[0] + b1 * line[1];

        final double a2 = line2[3] - line2[1];
        final double b2 = line2[0] - line2[2];
        final double c2 = a2 * line2[0] + b2 * line2[1];

        final double delta = a1 * b2 - a2 * b1;

        return new double[]{(b2 * c1 - b1 * c2) / delta, (a1 * c2 - a2 * c1) / delta};
    }
}
