/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class LocateCommand extends Command {

    private Vec3d firstStart;
    private Vec3d firstEnd;
    private Vec3d secondStart;
    private Vec3d secondEnd;

    private final List<Block> netherFortressBlocks = Arrays.asList(
            Blocks.NETHER_BRICKS,
            Blocks.NETHER_BRICK_FENCE,
            Blocks.NETHER_WART
    );

    private final List<Block> monumentBlocks = Arrays.asList(
            Blocks.PRISMARINE_BRICKS,
            Blocks.SEA_LANTERN,
            Blocks.DARK_PRISMARINE
    );

    private  final List<Block> strongholdBlocks = Arrays.asList(
            Blocks.END_PORTAL_FRAME
    );

    public LocateCommand() {
        super("locate", "Locates structures");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("buried_treasure").executes(s -> {
            ItemStack stack = mc.player.inventory.getMainHandStack();
            if (stack.getItem() != Items.FILLED_MAP) {
                ChatUtils.prefixError("Locate","You need to hold a treasure map first");
                return SINGLE_SUCCESS;
            }
            CompoundTag tag = stack.getTag();
            ListTag nbt1 = (ListTag) tag.get("Decorations");
            if (nbt1 == null) {
                ChatUtils.prefixError("Locate","Couldn't locate the cross. Are you holding a (highlight)treasure map(default)?");
                return SINGLE_SUCCESS;
            }

            CompoundTag iconNBT = nbt1.getCompound(0);
            if (iconNBT == null) {
                ChatUtils.prefixError("Locate","Couldn't locate the cross. Are you holding a (highlight)treasure map(default)?");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = new Vec3d(iconNBT.getDouble("x"),iconNBT.getDouble("y"),iconNBT.getDouble("z"));
            BaseText msg = new LiteralText("Buried Treasure located at ");
            msg.append(ChatUtils.formatCoords(coords));
            msg.append(".");
            ChatUtils.info("Locate", msg);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("mansion").executes(s -> {
            ItemStack stack = mc.player.inventory.getMainHandStack();
            if (stack.getItem() != Items.FILLED_MAP) {
                ChatUtils.prefixError("Locate","You need to hold a woodland explorer map first");
                return SINGLE_SUCCESS;
            }
            CompoundTag tag = stack.getTag();
            ListTag nbt1 = (ListTag) tag.get("Decorations");
            if (nbt1 == null) {
                ChatUtils.prefixError("Locate","Couldn't locate the mansion. Are you holding a (highlight)woodland explorer map(default)?");
                return SINGLE_SUCCESS;
            }

            CompoundTag iconNBT = nbt1.getCompound(0);
            if (iconNBT == null) {
                ChatUtils.prefixError("Locate","Couldn't locate the mansion. Are you holding a (highlight)woodland explorer map(default)?");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = new Vec3d(iconNBT.getDouble("x"),iconNBT.getDouble("y"),iconNBT.getDouble("z"));
            BaseText msg = new LiteralText("Mansion located at ");
            msg.append(ChatUtils.formatCoords(coords));
            msg.append(".");
            ChatUtils.info("Locate", msg);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stronghold").executes(s -> {
            if (InvUtils.findItemInHotbar(Items.ENDER_EYE) >= 0) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("follow entity minecraft:eye_of_ender");
                firstStart = null;
                firstEnd = null;
                secondStart = null;
                secondEnd = null;
                MeteorClient.EVENT_BUS.subscribe(this);
                ChatUtils.prefixInfo("Locate", "Please throw the first Eye of Ender");
                return SINGLE_SUCCESS;
            } else {
                Vec3d coords = findByBlockList(strongholdBlocks);
                if (coords == null ) {
                    ChatUtils.prefixError("Locate","No stronghold found nearby. You can use (highlight)Ender Eyes(default) for more success.");
                    return SINGLE_SUCCESS;
                }
                BaseText msg = new LiteralText("Stronghold located at ");
                msg.append(ChatUtils.formatCoords(coords));
                msg.append(".");
                ChatUtils.info("Locate", msg);
                return SINGLE_SUCCESS;
            }
        }));

        builder.then(literal("nether_fortress").executes(s -> {
            Vec3d coords = findByBlockList(netherFortressBlocks);
            if (coords == null ) {
                ChatUtils.prefixError("Locate","No nether fortress found.");
                return SINGLE_SUCCESS;
            }
            BaseText msg = new LiteralText("Nether fortress located at ");
            msg.append(ChatUtils.formatCoords(coords));
            msg.append(".");
            ChatUtils.info("Locate", msg);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("monument").executes(s -> {
            ItemStack stack = mc.player.inventory.getMainHandStack();
            if (stack.getItem() == Items.FILLED_MAP) {
                CompoundTag tag = stack.getTag();
                ListTag nbt1 = (ListTag) tag.get("Decorations");
                if (nbt1 != null) {
                    CompoundTag iconNBT = nbt1.getCompound(0);
                    if (iconNBT != null) {
                        Vec3d coords = new Vec3d(iconNBT.getDouble("x"),iconNBT.getDouble("y"),iconNBT.getDouble("z"));
                        BaseText msg = new LiteralText("Monument located at ");
                        msg.append(ChatUtils.formatCoords(coords));
                        msg.append(".");
                        ChatUtils.info("Locate", msg);
                        return SINGLE_SUCCESS;
                    }
                }
            }
            Vec3d coords = findByBlockList(monumentBlocks);
            if (coords == null ) {
                ChatUtils.prefixError("Locate","No monument found. You can try using a (highlight)Ocean explorer map(default) for more success.");
                return SINGLE_SUCCESS;
            }
            BaseText msg = new LiteralText("Monument located at ");
            msg.append(ChatUtils.formatCoords(coords));
            msg.append(".");
            ChatUtils.info("Locate", msg);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("cancel").executes(s -> {
            cancel();
            return SINGLE_SUCCESS;
        }));
    }

    private void cancel() {
        ChatUtils.prefixWarning("Locate","Locate canceled");
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    private Vec3d findByBlockList(List<Block> blockList) {
        List<BlockPos> posList = BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext(),
                blockList,64,10,32);
        if (posList.isEmpty()) {
            return null;
        }
        if (posList.size() < 3) {
            ChatUtils.prefixWarning("Locate","Only %d block(s) found. This search might be a false positive.", posList.size());
        }
        return new Vec3d(posList.get(0).getX(),posList.get(0).getY(),posList.get(0).getZ());
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySpawnS2CPacket) {
            EntitySpawnS2CPacket packet = (EntitySpawnS2CPacket) event.packet;
            if (packet.getEntityTypeId() == EntityType.EYE_OF_ENDER) {
                firstPosition(packet.getX(),packet.getY(),packet.getZ());
            }
        }
        if (event.packet instanceof PlaySoundS2CPacket) {
            PlaySoundS2CPacket packet = (PlaySoundS2CPacket) event.packet;
            if (packet.getSound() == SoundEvents.ENTITY_ENDER_EYE_DEATH) {
                lastPosition(packet.getX(), packet.getY(), packet.getZ());
            }
        }
    }

    private void firstPosition(double x, double y, double z) {
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstStart == null) {
            this.firstStart = pos;
        }
        else {
            this.secondStart = pos;
        }
    }

    private void lastPosition(double x, double y, double z) {
        ChatUtils.prefixInfo("Locate","%s Eye of Ender's trajectory saved.", (this.firstEnd==null)?"First":"Second");
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstEnd == null) {
            this.firstEnd = pos;
            ChatUtils.prefixInfo("Locate","Please throw the second Eye Of Ender from a different location.");
        }
        else {
            this.secondEnd = pos;
            findStronghold();
        }
    }

    private void findStronghold() {
        if (this.firstStart == null || this.firstEnd == null || this.secondStart == null || this.secondEnd == null) {
            ChatUtils.prefixError("Locate","Missing position data");
            cancel();
            return;
        }
        final double[] start = new double[]{this.secondStart.x, this.secondStart.z, this.secondEnd.x, this.secondEnd.z};
        final double[] end = new double[]{this.firstStart.x, this.firstStart.z, this.firstEnd.x, this.firstEnd.z};
        final double[] intersection = calcIntersection(start, end);
        if (Double.isNaN(intersection[0]) || Double.isNaN(intersection[1]) || Double.isInfinite(intersection[0]) || Double.isInfinite(intersection[1])) {
            ChatUtils.prefixError("Locate","Lines are parallel");
            cancel();
            return;
        }
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("stop");
        MeteorClient.EVENT_BUS.unsubscribe(this);
        Vec3d pos = new Vec3d(intersection[0],0,intersection[1]);
        BaseText msg = new LiteralText("Stronghold roughly located at ");
        msg.append(ChatUtils.formatCoords(pos));
        msg.append(".");
        ChatUtils.info("Locate", msg);
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
