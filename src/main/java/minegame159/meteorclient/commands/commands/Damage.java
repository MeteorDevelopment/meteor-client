package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.utils.player.ChatUtils;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Damage extends Command {

    public Damage() {
        super("damage", "Damages self");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("damage", IntegerArgumentType.integer(1, 7)).executes(context -> {
            int amount = context.getArgument("damage", Integer.class);
            if (mc.player.abilities.creativeMode) {
                ChatUtils.error("You are in creative");
                return SINGLE_SUCCESS;
            }
            damagePlayer(amount);
            return SINGLE_SUCCESS;
        }));

    }
    
    private void damagePlayer(int amount) {
        Vec3d pos = mc.player.getPos();
        NoFall nofall = Modules.get().get(NoFall.class);
        boolean nofallEnabled = nofall.isActive();
        nofall.toggle(false);
        for(int i = 0; i < 80; i++) {
            sendPosistionPacket(pos.x, pos.y + amount + 2.1, pos.z, false);
            sendPosistionPacket(pos.x, pos.y + 0.05, pos.z, false);
        }
        
        sendPosistionPacket(pos.x, pos.y, pos.z, true);
        nofall.toggle(nofallEnabled);

    }

    private void sendPosistionPacket(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y, z, onGround));
    }
}
