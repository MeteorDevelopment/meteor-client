package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.arguments.CompoundNbtTagArgumentType;
import minegame159.meteorclient.utils.player.Chat;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Nbt extends Command {
    public Nbt() {
        super("nbt", "Modifies NBT data for an item, example: .nbt add {display:{Name:'{\"text\":\"$cRed Name\"}'}}");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(argument("nbt_data", CompoundNbtTagArgumentType.nbtTag()).executes(s -> {
            ItemStack stack = mc.player.inventory.getMainHandStack();
            if (validBasic(stack)) {
                CompoundTag tag = s.getArgument("nbt_data", CompoundTag.class);
                stack.getTag().copyFrom(tag);
                setStack(stack);
            }
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("set").then(argument("nbt_data", CompoundNbtTagArgumentType.nbtTag()).executes(s -> {
            ItemStack stack = mc.player.inventory.getMainHandStack();
            if (validBasic(stack)) {
                CompoundTag tag = s.getArgument("nbt_data", CompoundTag.class);
                stack.setTag(tag);
                setStack(stack);
            }
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("remove").then(argument("nbt_path", NbtPathArgumentType.nbtPath()).executes(s -> {
            ItemStack stack = mc.player.inventory.getMainHandStack();
            if (validBasic(stack)) {
                NbtPathArgumentType.NbtPath path = s.getArgument("nbt_path", NbtPathArgumentType.NbtPath.class);
                path.remove(stack.getTag());
            }
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("get").executes(s -> {
            ItemStack stack = mc.player.inventory.getMainHandStack();
            if(stack == null) {
                Chat.error("You must hold an item in your main hand.");
            }
            else {
                Chat.info(stack.getTag().toString());
            }
            return SINGLE_SUCCESS;
        }));
    }

    private void setStack(ItemStack stack) {
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.inventory.selectedSlot, stack));
    }

    private boolean validBasic(ItemStack stack) {
        if(!mc.player.abilities.creativeMode) {
            Chat.error("Creative mode only.");
            return false;
        }

        if(stack == null) {
            Chat.error("You must hold an item in your main hand.");
            return false;
        }
        return true;
    }
}
