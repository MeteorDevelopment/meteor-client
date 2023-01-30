/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.world.GoalDirection;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class BaritoneUtils {
    public static String onSendChatMessage(String message) {
        if (!message.startsWith(Config.get().prefix.get()) && !message.startsWith(BaritoneAPI.getSettings().prefix.value)) {
            SendMessageEvent event = MeteorClient.EVENT_BUS.post(SendMessageEvent.get(message));
            if (!event.isCancelled())
                return message;
        }
        return null;
    }

    public static void cancelEverything(){
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public static void setGoalAndPath(GoalDirection goal) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
    }

    public static void GoalGetToBlock(BlockPos pos) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone.getPathingBehavior().isPathing()) baritone.getPathingBehavior().cancelEverything();
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(pos));
    }

    public static void GoalXZ(int x, int z) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
    }

    public static void follow(Predicate<Entity> entityPredicate) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess().follow(entityPredicate);
    }
}
