package minegame159.meteorclient.modules.combat;

//Created by squidoodly 25/04/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RightClickEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class OffhandExtra extends ToggleModule {
    public enum Mode{
        Enchanted_Golden_Apple,
        Golden_Apple,
        Exp_Bottle,
        End_Crystal,
    }

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public OffhandExtra(){ super(Category.Combat, "offhand-extra", "Allows you to use items in your offhand. Requires AutoTotem to be on smart mode."); }

    private final Setting<Mode> mode = addSetting(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .description("Changes which item goes in your offhand")
            .defaultValue(Mode.Enchanted_Golden_Apple)
            .build()
    );

    private final Setting<Boolean> replace = addSetting(new BoolSetting.Builder()
            .name("replace")
            .description("Replace your offhand or wait for it to be empty")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> Asimov = addSetting(new BoolSetting.Builder()
            .name("Asimov")
            .description("Always holds the item in your offhand")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> health = addSetting(new IntSetting.Builder()
            .name("health")
            .description("The health this stops working.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .build()
    );

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if(ModuleManager.INSTANCE.get(AutoTotem.class).getLocked()) return;
        if(Asimov.get() && !(mc.currentScreen instanceof ContainerScreen<?>)){
            Item item = getItem();
            InvUtils.FindItemResult result = InvUtils.findItemWithCount(item);
            if (mc.player.getOffHandStack().isEmpty()) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            }else if (mc.player.getOffHandStack().getItem() != item && replace.get()){
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
            }
        }
    });

    @EventHandler
    private final Listener<RightClickEvent> onRightClick = new Listener<>(event -> {
        if(ModuleManager.INSTANCE.get(AutoTotem.class).getLocked()) return;
        if((mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || (mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.get())
               && (mc.player.getOffHandStack().getItem() != getItem()) && !(mc.currentScreen instanceof ContainerScreen<?>))){
            Item item = getItem();
            InvUtils.FindItemResult result = InvUtils.findItemWithCount(item);
            if (mc.player.getOffHandStack().isEmpty()) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            }else if (mc.player.getOffHandStack().getItem() != item && replace.get()){
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
            }
        }
    });

    private Item getItem(){
        Item item = Items.TOTEM_OF_UNDYING;
        if (mode.get() == Mode.Enchanted_Golden_Apple) {
            item = Items.ENCHANTED_GOLDEN_APPLE;
        } else if (mode.get() == Mode.Golden_Apple) {
            item = Items.GOLDEN_APPLE;
        } else if (mode.get() == Mode.End_Crystal) {
            item = Items.END_CRYSTAL;
        } else if (mode.get() == Mode.Exp_Bottle) {
            item = Items.EXPERIENCE_BOTTLE;
        }
        return item;
    }

}
