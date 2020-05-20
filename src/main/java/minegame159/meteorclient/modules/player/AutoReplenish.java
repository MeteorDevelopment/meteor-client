package minegame159.meteorclient.modules.player;

//Created by squidoodly 8/05/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AutoReplenish extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("amount")
            .description("The amount this actives at")
            .defaultValue(32)
            .min(0)
            .sliderMax(63)
            .build()
    );

    private List<Item> items = new ArrayList<>();

    public AutoReplenish(){
        super(Category.Player, "auto-replenish", "Automatically fills your hotbar items");
    }

    @EventHandler
    private Listener<TickEvent> OnTick = new Listener<>(event -> {
        for(int i = 0; i < 9; i++){
            if(mc.player.inventory.getInvStack(i) == null || mc.currentScreen instanceof ContainerScreen
                    || !mc.player.inventory.getInvStack(i).isStackable()) return;
            if(mc.player.inventory.getInvStack(i).getCount() < amount.get()){
                int slot = findItems(mc.player.inventory.getInvStack(i).getItem());
                if(slot == -1 && !items.contains(mc.player.inventory.getInvStack(i).getItem())){
                    Utils.sendMessage("#redYou are out of #blue" + mc.player.inventory.getInvStack(i).getItem().toString() + "#red. Cannot refill.");
                    items.add(mc.player.inventory.getInvStack(i).getItem());
                    return;
                }
                if(slot == -1) return;
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(slot), 0, SlotActionType.PICKUP);
            }
        }
    });

    @EventHandler
    private Listener<OpenScreenEvent> OnScreen = new Listener<>(event -> {
        if(mc.currentScreen instanceof ContainerScreen){
            items.clear();
        }
    });

    private int findItems(Item item){
        int slot = -1;
        for(int i = 9; i < 45; i++){
            if(mc.player.inventory.getInvStack(i).getItem() == item){
                slot = i;
            }
        }
        return slot;
    }
}
