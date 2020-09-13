package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class ChestSwap extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> stayOn = sgGeneral.add(new BoolSetting.Builder()
            .name("stay-on")
            .description("Stays on and activates when you turn it off too.")
            .defaultValue(false)
            .build()
    );

    public ChestSwap() {
        super(Category.Player, "chest-swap", "Swaps between chestplate and elytra.");
    }

    @Override
    public void onActivate() {
        swap();
        if (!stayOn.get()) toggle();
    }

    @Override
    public void onDeactivate() {
        if (stayOn.get()) swap();
    }

    public void swap() {
        Item currentItem = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
        Item desiredItem = null;
        if (currentItem == Items.DIAMOND_CHESTPLATE) desiredItem = Items.ELYTRA;
        else if (currentItem == Items.ELYTRA) desiredItem = Items.DIAMOND_CHESTPLATE;

        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            Item item = mc.player.inventory.main.get(i).getItem();

            if (desiredItem == null && (item == Items.DIAMOND_CHESTPLATE || item == Items.ELYTRA)) {
                equip(i);
                break;
            } else if (item == desiredItem) {
                equip(i);
                break;
            }
        }
    }

    private void equip(int slot) {
        int chestSlot = 8 - 2;
        slot = InvUtils.invIndexToSlotId(slot);

        InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
        InvUtils.clickSlot(chestSlot, 0, SlotActionType.PICKUP);
        InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
    }

    @Override
    public void sendToggledMsg() {
        if (stayOn.get()) super.sendToggledMsg();
        else if (Config.INSTANCE.chatCommandsInfo) Chat.info("Triggered (highlight)%s(default).", title);
    }
}
