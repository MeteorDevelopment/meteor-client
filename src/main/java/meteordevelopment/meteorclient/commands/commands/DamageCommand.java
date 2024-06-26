/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.player.AntiHunger;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class DamageCommand extends Command {
    private final static SimpleCommandExceptionType INVULNERABLE = new SimpleCommandExceptionType(Text.literal("You are invulnerable."));

    public DamageCommand() {
        super("damage", "Damages self", "dmg");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("damage", IntegerArgumentType.integer(1, 7)).executes(context -> {
            int amount = IntegerArgumentType.getInteger(context, "damage");

            if (mc.player.getAbilities().invulnerable) {
                throw INVULNERABLE.create();
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
            sendPositionPacket(pos.x, pos.y + amount + 2.1, pos.z, false);
            sendPositionPacket(pos.x, pos.y + 0.05, pos.z, false);
        }

        sendPositionPacket(pos.x, pos.y, pos.z, true);

        if (noFall) Modules.get().get(NoFall.class).toggle();
        if (antiHunger) Modules.get().get(AntiHunger.class).toggle();
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
    }
}
