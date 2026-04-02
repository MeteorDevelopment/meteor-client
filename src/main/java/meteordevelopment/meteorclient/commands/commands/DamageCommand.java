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
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class DamageCommand extends Command {
    private final static SimpleCommandExceptionType INVULNERABLE = new SimpleCommandExceptionType(Component.literal("You are invulnerable."));

    public DamageCommand() {
        super("damage", "Damages self", "dmg");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
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

        Vec3 pos = mc.player.position();

        for (int i = 0; i < 80; i++) {
            sendPositionPacket(pos.x, pos.y + amount + 2.1, pos.z, false);
            sendPositionPacket(pos.x, pos.y + 0.05, pos.z, false);
        }

        sendPositionPacket(pos.x, pos.y, pos.z, true);

        if (noFall) Modules.get().get(NoFall.class).toggle();
        if (antiHunger) Modules.get().get(AntiHunger.class).toggle();
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, mc.player.horizontalCollision));
    }
}
