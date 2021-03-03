/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands.swarm;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.misc.Swarm;
import net.minecraft.command.CommandSource;

import java.util.Random;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmScatter extends Command {

    public SwarmScatter(){
        super("swarm","(highlight)scatter(default) - Send them running.");
    }


    public void scatter() {
        scatter(100);
    }

    public void scatter(int radius){
        if(mc.player != null) {
            Random random = new Random();
            double a = random.nextDouble() * 2 * Math.PI;
            double r = radius * Math.sqrt(random.nextDouble());
            double x = mc.player.getX() + r * Math.cos(a);
            double z = mc.player.getZ() + r * Math.sin(a);
            if(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int)x,(int)z));
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("scatter").executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if(swarm.isActive()){
                if(swarm.currentMode == Swarm.Mode.Queen && swarm.server != null){
                    swarm.server.sendMessage(context.getInput());
                }
                else{
                    scatter();
                }
            }
            return SINGLE_SUCCESS;
        }).then(argument("radius", IntegerArgumentType.integer()).executes(context -> {
            Swarm swarm = Modules.get().get(Swarm.class);
            if(swarm.isActive()){
                if(swarm.currentMode == Swarm.Mode.Queen && swarm.server != null){
                    swarm.server.sendMessage(context.getInput());
                }
                else{
                    scatter(context.getArgument("radius",Integer.class));
                }
            }
            return SINGLE_SUCCESS;
        })));
    }
}
