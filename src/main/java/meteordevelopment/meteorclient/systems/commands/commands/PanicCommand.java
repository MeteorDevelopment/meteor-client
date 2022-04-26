/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;
import meteordevelopment.meteorclient.systems.hud.HUD;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PanicCommand extends Command {
    public PanicCommand() {
        super("panic", "Turns all modules off.", "p");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
                Modules.get().disableAll();
                HUD.get().active = false;

                return SINGLE_SUCCESS;
        });
    }
}
