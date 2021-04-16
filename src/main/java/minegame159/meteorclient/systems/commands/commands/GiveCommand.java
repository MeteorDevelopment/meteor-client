package minegame159.meteorclient.systems.commands.commands;

//Created by Octoham 16/04/2021

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.NbtTagArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GiveCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(new LiteralText("You must be in creative mode to use this."));

    public GiveCommand() {
        super("give", "Gives you any item. REQUIRES Creative mode.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", ItemStackArgumentType.itemStack()).then(argument("number", IntegerArgumentType.integer()).then(argument("nbt", NbtTagArgumentType.nbtTag()).executes(context -> {
            if (!mc.player.isCreative()) throw NOT_IN_CREATIVE.create();

            ItemStack item = new ItemStack(context.getArgument("item", Item.class),context.getArgument("number", int.class));
            String nbt = context.getArgument("nbt", String.class);
			try
			{
				CompoundTag tag = StringNbtReader.parse(nbt);
				item.setTag(tag);
				
			}catch(CommandSyntaxException e)
			{
				SimpleCommandExceptionType commandExeption = new SimpleCommandExceptionType(new LiteralText(e.getMessage()));
				throw commandExeption.create();
			}
            Utils.addItem(item);
            
            return SINGLE_SUCCESS;
        }))));
    }
}
