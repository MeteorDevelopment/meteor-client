/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.EnumArgumentType;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.WorldGenUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
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

    private final static DynamicCommandExceptionType NOT_FOUND = new DynamicCommandExceptionType(o -> {
       if (o instanceof WorldGenUtils.Feature) {
           return new LiteralText(String.format(
               "%s not found.",
               Utils.nameToTitle(o.toString().replaceAll("_", "-")))
           );
       }
       return new LiteralText("Not found.");
    });

    private Vec3d firstStart;
    private Vec3d firstEnd;
    private Vec3d secondStart;
    private Vec3d secondEnd;

    public LocateCommand() {
        super("locate", "Locates structures", "loc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("lodestone").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() != Items.COMPASS) {
                error("You need to hold a lodestone compass");
                return SINGLE_SUCCESS;
            }
            NbtCompound tag = stack.getNbt();
            if (tag == null) {
                error("Couldn't get the NBT data. Are you holding a (highlight)lodestone(default) compass?");
                return SINGLE_SUCCESS;
            }
            NbtCompound nbt1 = tag.getCompound("LodestonePos");
            if (nbt1 == null) {
                error("Couldn't get the NBT data. Are you holding a (highlight)lodestone(default) compass?");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = new Vec3d(nbt1.getDouble("X"),nbt1.getDouble("Y"),nbt1.getDouble("Z"));
            BaseText text = new LiteralText("Lodestone located at ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("feature", EnumArgumentType.enumArgument(WorldGenUtils.Feature.stronghold)).executes(ctx -> {
            WorldGenUtils.Feature feature = EnumArgumentType.getEnum(ctx, "feature", WorldGenUtils.Feature.stronghold);
            BlockPos pos = WorldGenUtils.locateFeature(feature, mc.player.getBlockPos());
            if (pos != null) {
                BaseText text = new LiteralText(String.format(
                    "%s located at ",
                    Utils.nameToTitle(feature.toString().replaceAll("_", "-"))
                ));
                Vec3d coords = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                text.append(ChatUtils.formatCoords(coords));
                text.append(".");
                info(text);
                return SINGLE_SUCCESS;
            }
            if (feature == WorldGenUtils.Feature.stronghold) {
                FindItemResult eye = InvUtils.findInHotbar(Items.ENDER_EYE);
                if (eye.found()) {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("follow entity minecraft:eye_of_ender");
                    firstStart = null;
                    firstEnd = null;
                    secondStart = null;
                    secondEnd = null;
                    MeteorClient.EVENT_BUS.subscribe(this);
                    info("Please throw the first Eye of Ender.");
                    BaseText text = new LiteralText("Fortress located at ");
                    text.append(ChatUtils.formatCoords(coords));
                    text.append(".");
                    info(text);
                    return SINGLE_SUCCESS;
                }
            }
            throw NOT_FOUND.create(feature);
        }));

        builder.then(literal("cancel").executes(s -> {
            cancel();
            return SINGLE_SUCCESS;
        }));
    }

    private void cancel() {
        warning("Locate canceled.");
        MeteorClient.EVENT_BUS.unsubscribe(this);
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
        info("%s Eye of Ender's trajectory saved.", (this.firstEnd == null) ? "First" : "Second");
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstEnd == null) {
            this.firstEnd = pos;
            info("Please throw the second Eye Of Ender from a different location.");
        }
        else {
            this.secondEnd = pos;
            findStronghold();
        }
    }

    private void findStronghold() {
        if (this.firstStart == null || this.firstEnd == null || this.secondStart == null || this.secondEnd == null) {
            error("Missing position data");
            cancel();
            return;
        }
        final double[] start = new double[]{this.secondStart.x, this.secondStart.z, this.secondEnd.x, this.secondEnd.z};
        final double[] end = new double[]{this.firstStart.x, this.firstStart.z, this.firstEnd.x, this.firstEnd.z};
        final double[] intersection = calcIntersection(start, end);
        if (Double.isNaN(intersection[0]) || Double.isNaN(intersection[1]) || Double.isInfinite(intersection[0]) || Double.isInfinite(intersection[1])) {
            error("Lines are parallel.");
            cancel();
            return;
        }
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("stop");
        MeteorClient.EVENT_BUS.unsubscribe(this);
        Vec3d coords = new Vec3d(intersection[0],0,intersection[1]);
        BaseText text = new LiteralText("Stronghold roughly located at ");
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
