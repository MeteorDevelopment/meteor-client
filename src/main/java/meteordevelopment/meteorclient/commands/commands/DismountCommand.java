/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;

public class DismountCommand extends Command {
    public DismountCommand() {
        super("dismount", "Dismounts you from entity you are riding.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            Input sneak = new Input(false, false, false, false, false, true, false);
            mc.getConnection().send(new ServerboundPlayerInputPacket(sneak));
            return SINGLE_SUCCESS;
        });
    }
}
