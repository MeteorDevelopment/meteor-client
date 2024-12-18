/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
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
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LocateCommand extends Command {
    private Vec3d firstStart;
    private Vec3d firstEnd;
    private Vec3d secondStart;
    private Vec3d secondEnd;

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
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // Overworld structures

        builder.then(literal("buried_treasure").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() != Items.FILLED_MAP
                || stack.get(DataComponentTypes.ITEM_NAME) == null
                || !stack.get(DataComponentTypes.ITEM_NAME).getString().equals(Text.translatable("filled_map.buried_treasure").getString())) {
                error("You need to hold a (highlight)buried treasure map(default)!");
                return SINGLE_SUCCESS;
            }

            MapDecorationsComponent mapDecorationsComponent = stack.get(DataComponentTypes.MAP_DECORATIONS);
            if (mapDecorationsComponent == null) {
                error("Couldn't locate the map icons!");
                return SINGLE_SUCCESS;
            }

            for (MapDecorationsComponent.Decoration decoration : mapDecorationsComponent.decorations().values()) {
                if (decoration.type().value().assetId().toString().equals("minecraft:red_x")) {
                    Vec3d coords = new Vec3d(decoration.x(), 62, decoration.z());
                    MutableText text = Text.literal("Buried Treasure located at ");
                    text.append(ChatUtils.formatCoords(coords));
                    text.append(".");
                    info(text);
                    return SINGLE_SUCCESS;
                }
            }

            error("Couldn't locate the buried treasure!");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("mansion").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() != Items.FILLED_MAP
                || stack.get(DataComponentTypes.ITEM_NAME) == null
                || !stack.get(DataComponentTypes.ITEM_NAME).getString().equals(Text.translatable("filled_map.mansion").getString())) {
                error("You need to hold a (highlight)woodland explorer map(default)!");
                return SINGLE_SUCCESS;
            }

            MapDecorationsComponent mapDecorationsComponent = stack.get(DataComponentTypes.MAP_DECORATIONS);
            if (mapDecorationsComponent == null) {
                error("Couldn't locate the map icons!");
                return SINGLE_SUCCESS;
            }

            for (MapDecorationsComponent.Decoration decoration : mapDecorationsComponent.decorations().values()) {
                if (decoration.type().value().assetId().toString().equals("minecraft:woodland_mansion")) {
                    Vec3d coords = new Vec3d(decoration.x(), 62, decoration.z());
                    MutableText text = Text.literal("Mansion located at ");
                    text.append(ChatUtils.formatCoords(coords));
                    text.append(".");
                    info(text);
                    return SINGLE_SUCCESS;
                }
            }

            error("Couldn't locate the mansion!");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("monument").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() == Items.FILLED_MAP
                && stack.get(DataComponentTypes.ITEM_NAME) != null
                && stack.get(DataComponentTypes.ITEM_NAME).getString().equals(Text.translatable("filled_map.monument").getString())) {

                MapDecorationsComponent mapDecorationsComponent = stack.get(DataComponentTypes.MAP_DECORATIONS);
                if (mapDecorationsComponent == null) {
                    error("Couldn't locate the map icons!");
                    return SINGLE_SUCCESS;
                }

                for (MapDecorationsComponent.Decoration decoration : mapDecorationsComponent.decorations().values()) {
                    if (decoration.type().value().assetId().toString().equals("minecraft:ocean_monument")) {
                        Vec3d coords = new Vec3d(decoration.x(), 62, decoration.z());
                        MutableText text = Text.literal("Monument located at ");
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
                Vec3d coords = findByBlockList(monumentBlocks);
                if (coords == null) {
                    error("No monument found. Try using an (highlight)ocean explorer map(default) for more success.");
                    return SINGLE_SUCCESS;
                }
                MutableText text = Text.literal("Monument located at ");
                text.append(ChatUtils.formatCoords(coords));
                text.append(".");
                info(text);
                return SINGLE_SUCCESS;
            }

            error("Locating this structure without an (highlight)ocean explorer map(default) requires Baritone.");
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
                info("Please throw the first Eye of Ender");
            } else if (BaritoneUtils.IS_AVAILABLE) {
                Vec3d coords = findByBlockList(strongholdBlocks);
                if (coords == null) {
                    error("No stronghold found nearby. You can use (highlight)Ender Eyes(default) for more success.");
                    return SINGLE_SUCCESS;
                }
                MutableText text = Text.literal("Stronghold located at ");
                text.append(ChatUtils.formatCoords(coords));
                text.append(".");
                info(text);
            } else {
                error("No Eyes of Ender found in hotbar.");
            }

            return SINGLE_SUCCESS;
        }));

        // Nether structures

        builder.then(literal("nether_fortress").executes(s -> {
            if (mc.world.getRegistryKey() != World.NETHER) {
                error("You need to be in the nether to locate a nether fortress.");
                return SINGLE_SUCCESS;
            }

            if (!BaritoneUtils.IS_AVAILABLE) {
                error("Locating this structure requires Baritone.");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = findByBlockList(netherFortressBlocks);
            if (coords == null) {
                error("No nether fortress found.");
                return SINGLE_SUCCESS;
            }
            MutableText text = Text.literal("Fortress located at ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        // End structures

        builder.then(literal("end_city").executes(s -> {
            if (mc.world.getRegistryKey() != World.END) {
                error("You need to be in the end to locate an end city.");
                return SINGLE_SUCCESS;
            }

            if (!BaritoneUtils.IS_AVAILABLE) {
                error("Locating this structure requires Baritone.");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = findByBlockList(endCityBlocks);
            if (coords == null) {
                error("No end city found.");
                return SINGLE_SUCCESS;
            }
            MutableText text = Text.literal("End city located at ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        // Misc structures

        builder.then(literal("lodestone").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() != Items.COMPASS) {
                error("You need to hold a (highlight)lodestone(default) compass!");
                return SINGLE_SUCCESS;
            }
            ComponentMap components = stack.getComponents();
            if (components == null) {
                error("Couldn't get the components data. Are you holding a (highlight)lodestone(default) compass?");
                return SINGLE_SUCCESS;
            }
            LodestoneTrackerComponent lodestoneTrackerComponent = components.get(DataComponentTypes.LODESTONE_TRACKER);
            if (lodestoneTrackerComponent == null) {
                error("Couldn't get the components data. Are you holding a (highlight)lodestone(default) compass?");
                return SINGLE_SUCCESS;
            }

            if (lodestoneTrackerComponent.target().isEmpty()) {
                error("Couldn't get the lodestone's target!");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = Vec3d.of(lodestoneTrackerComponent.target().get().pos());
            MutableText text = Text.literal("Lodestone located at ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("cancel").executes(s -> {
            cancel();
            return SINGLE_SUCCESS;
        }));
    }

    private void cancel() {
        warning("Locate canceled");
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    private @Nullable Vec3d findByBlockList(List<Block> blockList) {
        List<BlockPos> posList = BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext(), blockList, 64, 10, 32);
        if (posList.isEmpty()) {
            return null;
        }
        if (posList.size() < 3) {
            warning("Only %d block(s) found. This search might be a false positive.", posList.size());
        }
        return new Vec3d(posList.getFirst().getX(), posList.getFirst().getY(), posList.getFirst().getZ());
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySpawnS2CPacket packet && packet.getEntityType() == EntityType.EYE_OF_ENDER) {
            firstPosition(packet.getX(), packet.getY(), packet.getZ());
        }

        if (event.packet instanceof PlaySoundS2CPacket packet && packet.getSound().value() == SoundEvents.ENTITY_ENDER_EYE_DEATH) {
            lastPosition(packet.getX(), packet.getY(), packet.getZ());
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
        info("%s Eye of Ender's trajectory saved.", (this.firstEnd == null) ? "First" : "Second");
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstEnd == null) {
            this.firstEnd = pos;
            info("Please throw the second Eye Of Ender from a different location.");
        } else {
            this.secondEnd = pos;
            findStronghold();
        }
    }

    private void findStronghold() {
        PathManagers.get().stop();

        if (this.firstStart == null || this.firstEnd == null || this.secondStart == null || this.secondEnd == null) {
            error("Missing position data");
            cancel();
            return;
        }

        final double[] start = new double[]{this.secondStart.x, this.secondStart.z, this.secondEnd.x, this.secondEnd.z};
        final double[] end = new double[]{this.firstStart.x, this.firstStart.z, this.firstEnd.x, this.firstEnd.z};
        final double[] intersection = calcIntersection(start, end);
        if (Double.isNaN(intersection[0]) || Double.isNaN(intersection[1]) || Double.isInfinite(intersection[0]) || Double.isInfinite(intersection[1])) {
            error("Unable to calculate intersection.");
            cancel();
            return;
        }

        MeteorClient.EVENT_BUS.unsubscribe(this);
        Vec3d coords = new Vec3d(intersection[0], 0, intersection[1]);
        MutableText text = Text.literal("Stronghold roughly located at ");
        text.append(ChatUtils.formatCoords(coords));
        text.append(".");
        info(text);
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
