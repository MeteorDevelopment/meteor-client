package minegame159.meteorclient.modules.creative;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;

public class Sword32k extends GiveItemModule {
    public Sword32k() {
        super("32k-sword", "Gives a sword with ridiculous enchantments");
    }

    @Override
    ItemStack getStack() {
        ItemStack stack = new ItemStack(Items.NETHERITE_SWORD);
        ListTag enchants = new ListTag();
        addEnchant(enchants, "minecraft:sharpness");
        addEnchant(enchants, "minecraft:knockback");
        addEnchant(enchants, "minecraft:fire_aspect");
        addEnchant(enchants, "minecraft:looting", (short)10);
        addEnchant(enchants, "minecraft:sweeping", (short)3);
        addEnchant(enchants, "minecraft:unbreaking");
        addEnchant(enchants, "minecraft:mending", (short)1);
        addEnchant(enchants, "minecraft:vanishing_curse", (short)1);
        CompoundTag nbt = new CompoundTag();
        nbt.put("Enchantments", enchants);
        stack.setTag(nbt);
        stack.setCustomName(new LiteralText("Bonk"));
        return stack;
    }

    private void addEnchant(ListTag tag, String id) {
        addEnchant(tag, id, Short.MAX_VALUE);
    }

    private void addEnchant(ListTag tag, String id, short v) {
        CompoundTag enchant = new CompoundTag();
        enchant.putShort("lvl", v);
        enchant.putString("id", id);
        tag.add(enchant);
    }
}
