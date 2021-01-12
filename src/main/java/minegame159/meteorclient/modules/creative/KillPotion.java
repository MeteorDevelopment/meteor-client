package minegame159.meteorclient.modules.creative;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;

public class KillPotion extends GiveItemModule {
    public KillPotion() {
        super("kill-potion", "Gives a potion that kills anything (includes creative mode players)");
    }

    @Override
    ItemStack getStack() {
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        CompoundTag effect = new CompoundTag();
        effect.putInt("Amplifier", 125);
        effect.putInt("Duration", 2000);
        effect.putInt("Id", 6);
        ListTag effects = new ListTag();
        effects.add(effect);
        CompoundTag nbt = new CompoundTag();
        nbt.put("CustomPotionEffects", effects);
        stack.setTag(nbt);
        String name = "\u00a7rSplash Potion of \u00a74\u00a7lINSTANT DEATH";
        stack.setCustomName(new LiteralText(name));
        return stack;
    }
}
