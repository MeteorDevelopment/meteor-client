package minegame159.meteorclient.modules.player;

//Created by squidoodly 03/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;

public class AutoGap extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public AutoGap(){
        super(Category.Player, "auto-gap", "Automatically eats gapples and egaps if their effects run out.");
    }

    private Setting<Boolean> preferEgap = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-egap")
            .description("Prefers to eat egaps over normal gapples")
            .defaultValue(false)
            .build()
    );

    private Setting<Boolean> preferAutoEat = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-auto-eat")
            .description("Whether to use auto-eat or this in the event of a conflict")
            .defaultValue(true)
            .build()
    );

    private boolean hadEaten = false;

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if(mc.options.keyUse.isPressed() && ModuleManager.INSTANCE.get(AutoEat.class).isActive() && preferAutoEat.get()){
            return;
        }else if(mc.options.keyUse.isPressed() && ModuleManager.INSTANCE.get(AutoEat.class).isActive() && !preferAutoEat.get()){
            ModuleManager.INSTANCE.get(AutoEat.class).toggle();
        }
        if(mc.player.getStatusEffects().contains(StatusEffects.ABSORPTION) && mc.player.getStatusEffects().contains(StatusEffects.REGENERATION)){
            if(mc.options.keyUse.isPressed()) ((IKeyBinding) mc.options.keyUse).setPressed(false);
            hadEaten = true;
        }else if(hadEaten){
            hadEaten = false;
            int gappleSlot = -1;
            int egapSlot = -1;
            for(int i = 0; i < 9; i++){
                if(mc.player.inventory.getInvStack(i).getItem() == Items.GOLDEN_APPLE && gappleSlot == -1){
                    gappleSlot = i;
                }else if(mc.player.inventory.getInvStack(i).getItem() == Items.ENCHANTED_GOLDEN_APPLE && egapSlot == -1){
                    egapSlot = i;
                }
            }
            if(preferEgap.get()){
                mc.player.inventory.selectedSlot = gappleSlot;
            }else{
                mc.player.inventory.selectedSlot = egapSlot;
            }
            ((IKeyBinding) mc.options.keyUse).setPressed(true);
        }
    });
}
