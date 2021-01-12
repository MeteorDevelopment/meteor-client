package minegame159.meteorclient.modules.creative;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.player.Chat;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

public abstract class GiveItemModule extends Module {
    public GiveItemModule(String name, String description) {
        super(Category.Creative, name, description);
    }

    @Override
    public void doAction(boolean onActivateDeactivate) {
        if (mc.player == null) {
            MeteorClient.LOG.warn("GiveItem modules may only be used in a world");
            return;
        }

        if(!mc.player.abilities.creativeMode)
        {
            Chat.error("Creative mode only.");
            return;
        }

        for(int i = 0; i < 9; i++)
        {
            if(!mc.player.inventory.getStack(i).isEmpty()) continue;

            mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + i, getStack()));
            Chat.info("Item created.");
            return;
        }

        Chat.error("Please clear a slot in your hotbar.");
    }

    abstract ItemStack getStack();
}
