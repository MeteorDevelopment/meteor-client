/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.starscript.StandardLib;
import meteordevelopment.starscript.Starscript;
import meteordevelopment.starscript.utils.Error;
import meteordevelopment.starscript.utils.StarscriptError;
import meteordevelopment.starscript.value.Value;
import meteordevelopment.starscript.value.ValueMap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorStarscript {
    public static Starscript ss = new Starscript();

    @Init(stage = InitStage.Pre)
    public static void init() {
        StandardLib.init(ss);

        // General
        ss.set("version", Value.string(MeteorClient.version != null ? (MeteorClient.devBuild.isEmpty() ? MeteorClient.version.toString() : MeteorClient.version + " " + MeteorClient.devBuild) : ""));
        ss.set("mc_version", Value.string(SharedConstants.getGameVersion().getName()));
        ss.set("fps", () -> Value.number(MinecraftClientAccessor.getFps()));

        // Player
        ss.set("player", Value.map(new ValueMap()
            .set("_toString", () -> Value.string(mc.getSession().getUsername()))
            .set("health", () -> Value.number(mc.player != null ? mc.player.getHealth() : 0))
            .set("hunger", () -> Value.number(mc.player != null ? mc.player.getHungerManager().getFoodLevel() : 0))
            .set("speed", () -> Value.number(Utils.getPlayerSpeed()))

            .set("pos", Value.map(new ValueMap()
                .set("_toString", MeteorStarscript::playerPosString)
                .set("x", () -> Value.number(mc.player != null ? mc.player.getX() : 0))
                .set("y", () -> Value.number(mc.player != null ? mc.player.getY() : 0))
                .set("z", () -> Value.number(mc.player != null ? mc.player.getZ() : 0))
            ))
            .set("yaw", () -> Value.number(mc.player != null ? mc.player.getYaw() : 0))
            .set("pitch", () -> Value.number(mc.player != null ? mc.player.getPitch() : 0))

            .set("hand", () -> mc.player != null ? wrap(mc.player.getMainHandStack()) : Value.null_())
            .set("offhand", () -> mc.player != null ? wrap(mc.player.getOffHandStack()) : Value.null_())
            .set("get_item", Value.function(MeteorStarscript::getItem))
            .set("count_items", Value.function(MeteorStarscript::countItems))
        ));

        // Crosshair target
        ss.set("crosshair_target", Value.map(new ValueMap()
            .set("type", MeteorStarscript::crosshairType)
            .set("value", MeteorStarscript::crosshairValue)
        ));

        // Server
        ss.set("server", Value.map(new ValueMap()
            .set("_toString", () -> Value.string(Utils.getWorldName()))
            .set("tps", () -> Value.number(TickRate.INSTANCE.getTickRate()))
            .set("time", () -> Value.string(Utils.getWorldTime()))
            .set("player_count", () -> Value.number(mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 0))
            .set("difficulty", () -> Value.string(mc.world != null ? mc.world.getDifficulty().getName() : ""))
        ));
    }

    public static void printChatError(int i, Error error) {
        if (i != -1) ChatUtils.error("Starscript", "%d, %d '%c': %s", i, error.character, error.ch, error.message);
        else ChatUtils.error("Starscript", "%d '%c': %s", error.character, error.ch, error.message);
    }

    public static void printChatError(Error error) {
        printChatError(-1, error);
    }

    public static void printChatError(StarscriptError e) {
        ChatUtils.error("Starscript", e.getMessage());
    }

    // Functions

    private static Value getItem(Starscript ss, int argCount) {
        if (argCount != 1) ss.error("player.get_item() requires 1 argument, got %d.", argCount);

        int i = (int) ss.popNumber("First argument to player.get_item() needs to be a number.");
        return mc.player != null ? wrap(mc.player.getInventory().getStack(i)) : Value.null_();
    }

    private static Value countItems(Starscript ss, int argCount) {
        if (argCount != 1) ss.error("player.count_items() requires 1 argument, got %d.", argCount);

        String idRaw = ss.popString("First argument to player.count_items() needs to be a string.");
        Identifier id = Identifier.tryParse(idRaw);
        if (id == null) return Value.number(0);

        Item item = Registry.ITEM.get(id);
        if (item == Items.AIR || mc.player == null) return Value.number(0);

        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.getItem() == item) count += itemStack.getCount();
        }

        return Value.number(count);
    }

    // Other

    private static Value playerPosString() {
        if (mc.player == null) return Value.string("X: 0 Y: 0 Z: 0");
        return posString(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private static Value posString(double x, double y, double z) {
        return Value.string(String.format("X: %.0f Y: %.0f Z: %.0f", x, y, z));
    }

    private static Value crosshairType() {
        if (mc.crosshairTarget == null) return Value.string("miss");

        return Value.string(switch (mc.crosshairTarget.getType()) {
            case MISS -> "miss";
            case BLOCK -> "block";
            case ENTITY -> "entity";
        });
    }

    private static Value crosshairValue() {
        if (mc.crosshairTarget == null) return Value.null_();

        if (mc.crosshairTarget.getType() == HitResult.Type.MISS) return Value.string("");
        if (mc.crosshairTarget instanceof BlockHitResult hit)
            return wrap(hit.getBlockPos(), mc.world.getBlockState(hit.getBlockPos()));
        return wrap(((EntityHitResult) mc.crosshairTarget).getEntity());
    }

    // Wrapping

    private static Value wrap(ItemStack itemStack) {
        String name = itemStack.isEmpty() ? "" : Names.get(itemStack.getItem());

        return Value.map(new ValueMap()
            .set("_toString", Value.string(itemStack.getCount() <= 1 ? name : String.format("%s %dx", name, itemStack.getCount())))
            .set("name", Value.string(name))
            .set("count", Value.number(itemStack.getCount()))
        );
    }

    private static Value wrap(BlockPos blockPos, BlockState blockState) {
        return Value.map(new ValueMap()
            .set("_toString", Value.string(Names.get(blockState.getBlock())))
            .set("pos", Value.map(new ValueMap()
                .set("_toString", posString(blockPos.getX(), blockPos.getY(), blockPos.getZ()))
                .set("x", Value.number(blockPos.getX()))
                .set("y", Value.number(blockPos.getY()))
                .set("z", Value.number(blockPos.getZ()))
            ))
        );
    }

    private static Value wrap(Entity entity) {
        return Value.map(new ValueMap()
            .set("_toString", Value.string(entity.getName().getString()))
            .set("health", Value.number(entity instanceof LivingEntity e ? e.getHealth() : 0))
            .set("pos", Value.map(new ValueMap()
                .set("_toString", posString(entity.getX(), entity.getY(), entity.getZ()))
                .set("x", Value.number(entity.getX()))
                .set("y", Value.number(entity.getY()))
                .set("z", Value.number(entity.getZ()))
            ))
        );
    }
}
