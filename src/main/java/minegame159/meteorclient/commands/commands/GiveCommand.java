package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.player.ChatUtils;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

// Using "Give" as class name breaks CrashReportMixin
public class GiveCommand extends Command {

    public GiveCommand() {
        super("give", "Gives items in creative mode.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", ItemStackArgumentType.itemStack()).executes(context -> {
            if (!mc.player.abilities.creativeMode) {
                ChatUtils.error("Not in creative mode.");
                return SINGLE_SUCCESS;
            }
            ItemStack item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem().getDefaultStack();
            placeStackInHotbar(item);
            return SINGLE_SUCCESS;
        }));

    }

    private void placeStackInHotbar(ItemStack stack)
	{
		for(int i = 0; i < 9; i++)
		{
			if(!mc.player.inventory.getStack(i).isEmpty())continue;
			
			mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + i, stack));
            return;
		}
		
		ChatUtils.error("No space in hotbar.");
	}
}
