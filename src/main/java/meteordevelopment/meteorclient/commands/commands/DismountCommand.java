/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;

public class DismountCommand extends Command {
    public DismountCommand() {
        super("dismount", "Dismounts you from entity you are riding.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0, 0, false, true));
            return SINGLE_SUCCESS;
        });
    }
}
