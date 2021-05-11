package minegame159.meteorclient.systems.commands.commands;

//Created by Octoham 16/04/2021

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.player.SlotUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GiveCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(new LiteralText("You must be in creative mode to use this."));

    public GiveCommand() {
        super("give", "Gives you any item. REQUIRES Creative mode.", "item");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", ItemStackArgumentType.itemStack()).executes(context -> {
            if (!mc.player.isCreative()) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
            addItem(item);
            
            return SINGLE_SUCCESS;
        }).then(argument("number", IntegerArgumentType.integer()).executes(context -> {
            if (!mc.player.isCreative()) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(IntegerArgumentType.getInteger(context, "number"), false);
            addItem(item);

            return SINGLE_SUCCESS;
        })));
    }

    private void addItem(ItemStack item) {
		for(int i = 0; i < 36; i++) {
		    ItemStack stack = mc.player.inventory.getStack(SlotUtils.indexToId(i));
			if (!stack.isEmpty()) continue;
			mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(SlotUtils.indexToId(i), item));
			return;
		}
    }
}
