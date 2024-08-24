/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.baritone;

import baritone.api.pathing.goals.GoalBlock;
import baritone.command.defaults.ComeCommand;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ComeCommand.class)
public abstract class ComeCommandMixin {
    @ModifyArgs(method = "execute", at = @At(value = "INVOKE", target = "Lbaritone/api/process/ICustomGoalProcess;setGoalAndPath(Lbaritone/api/pathing/goals/Goal;)V"), remap = false)
    private void getComeCommandTarget(Args args) {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam.isActive()) {
            float tickDelta = mc.getRenderTickCounter().getTickDelta(true); // todo unsure if true or false
            args.set(0, new GoalBlock((int) freecam.getX(tickDelta), (int) freecam.getY(tickDelta), (int) freecam.getZ(tickDelta)));
        }
    }
}
