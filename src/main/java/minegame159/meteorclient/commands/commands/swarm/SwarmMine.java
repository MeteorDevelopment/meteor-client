/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands.swarm;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.Swarm;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmMine extends Command {

    public SwarmMine() {
        super("swarm", "(highlight)mine <playername>(default) - Baritone Mine A Block");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("mine")
                .then(argument("block", BlockStateArgumentType.blockState())
                        .executes(context -> {
                            try {
                                Swarm swarm = ModuleManager.INSTANCE.get(Swarm.class);
                                if (swarm.isActive()) {
                                    if (swarm.currentMode == Swarm.Mode.Queen && swarm.server != null)
                                        swarm.server.sendMessage(context.getInput());
                                    if (swarm.currentMode != Swarm.Mode.Queen) {
                                        swarm.targetBlock = context.getArgument("block",BlockStateArgument.class).getBlockState();
                                    } else ChatUtils.moduleError(ModuleManager.INSTANCE.get(Swarm.class),"Null block");
                                }
                            } catch (Exception e) {
                                ChatUtils.moduleError(ModuleManager.INSTANCE.get(Swarm.class),"Error in baritone command. " + e.getClass().getSimpleName());
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );
    }
}
