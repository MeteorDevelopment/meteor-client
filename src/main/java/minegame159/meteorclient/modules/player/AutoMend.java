package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;

public class AutoMend extends ToggleModule {
    private Setting<Boolean> swords = addSetting(new BoolSetting.Builder()
            .name("swords")
            .description("Move swords.")
            .defaultValue(true)
            .build()
    );

    public AutoMend() {
        super(Category.Player, "auto-mend", "Automatically replaces items in offhand with mending when fully repaired.");
    }

    private void replaceItem(boolean offhandEmpty) {
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getInvStack(i);
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) == 0 || !itemStack.isDamaged()) continue;
            if (!swords.get() && itemStack.getItem() instanceof SwordItem) continue;

            InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            if (!offhandEmpty) InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 0, SlotActionType.PICKUP);

            break;
        }
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof ContainerScreen) return;

        if (mc.player.getOffHandStack().isEmpty()) replaceItem(true);
        else if (!mc.player.getOffHandStack().isDamaged()) replaceItem(false);
        else if (EnchantmentHelper.getLevel(Enchantments.MENDING, mc.player.getOffHandStack()) == 0) replaceItem(false);
    });
}
