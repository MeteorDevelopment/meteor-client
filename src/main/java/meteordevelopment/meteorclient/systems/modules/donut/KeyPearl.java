package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class KeyPearl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> pearlKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("pearl-key")
        .description("Key to throw an ender pearl.")
        .defaultValue(Keybind.none())
        .action(this::throwPearl)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Return to the item you were holding after throwing.")
        .defaultValue(true)
        .build()
    );

    public KeyPearl() {
        super(Categories.Donut, "key-pearl", "Throw an ender pearl from your hotbar with a keybind.");
    }

    private void throwPearl() {
        if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null) return;

        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        if (!pearl.found()) {
            info("No ender pearl in your hotbar.");
            return;
        }

        if (pearl.isOffhand()) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            return;
        }

        InvUtils.swap(pearl.slot(), swapBack.get());
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if (swapBack.get()) InvUtils.swapBack();
    }
}
