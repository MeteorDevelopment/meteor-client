package minegame159.meteorclient.modules.creative;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;

public class TrollPotion extends GiveItemModule {
    public TrollPotion() {
        super("troll-potion", "Gives a potion with a variety of annoying effects");
    }

    @Override
    ItemStack getStack() {
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        ListTag effects = new ListTag();
        for(int i = 1; i <= 23; i++)
        {
            CompoundTag effect = new CompoundTag();
            effect.putInt("Amplifier", Integer.MAX_VALUE);
            effect.putInt("Duration", Integer.MAX_VALUE);
            effect.putInt("Id", i);
            effects.add(effect);
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("CustomPotionEffects", effects);
        stack.setTag(nbt);
        String name = "\u00a7rSplash Potion of Trolling";
        stack.setCustomName(new LiteralText(name));
        return stack;
    }
}
