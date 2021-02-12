/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.commands.commands.swarm;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.combat.Swarm;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SwarmCloseConnections extends Command {

    public SwarmCloseConnections(){
        super("swarm","(highlight)close(default) - Close all network connections and cancel all tasks.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("close").executes(context -> {
                    try {
                        Swarm swarm = Modules.get().get(Swarm.class);
                        if(swarm.isActive()) {
                            swarm.closeAllServerConnections();
                            swarm.currentMode = Swarm.Mode.Idle;
                            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                            if (Modules.get().isActive(Swarm.class))
                                Modules.get().get(Swarm.class).toggle();
                        }
                    } catch (Exception ignored) {
                    }
                    return SINGLE_SUCCESS;
                })
        );
    }
}
