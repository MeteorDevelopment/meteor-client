/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.modules.player.AntiHunger;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class DamageCommand extends Command {
    public DamageCommand() {
        super("damage", "Damages self", "dmg");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("damage", IntegerArgumentType.integer(1, 7)).executes(context -> {
            int amount = context.getArgument("damage", Integer.class);

            if (mc.player.abilities.invulnerable) {
                ChatUtils.error("You are in invulnerable.");
                return SINGLE_SUCCESS;
            }

            damagePlayer(amount);
            return SINGLE_SUCCESS;
        }));

    }
    
    private void damagePlayer(int amount) {
        boolean noFall = Modules.get().isActive(NoFall.class);
        if (noFall) Modules.get().get(NoFall.class).toggle();

        boolean antiHunger = Modules.get().isActive(AntiHunger.class);
        if (antiHunger) Modules.get().get(AntiHunger.class).toggle();

        Vec3d pos = mc.player.getPos();

        for(int i = 0; i < 80; i++) {
            sendPosistionPacket(pos.x, pos.y + amount + 2.1, pos.z, false);
            sendPosistionPacket(pos.x, pos.y + 0.05, pos.z, false);
        }
        
        sendPosistionPacket(pos.x, pos.y, pos.z, true);

        if (noFall) Modules.get().get(NoFall.class).toggle();
        if (antiHunger) Modules.get().get(AntiHunger.class).toggle();
    }

    private void sendPosistionPacket(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y, z, onGround));
    }
}
