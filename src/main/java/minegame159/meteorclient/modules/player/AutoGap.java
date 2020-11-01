package minegame159.meteorclient.modules.player;

//Created by squidoodly 03/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.combat.CrystalAura;
import minegame159.meteorclient.modules.combat.KillAura;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;

public class AutoGap extends ToggleModule {
    public enum Mode{
        Fire_Resistance,
        Regeneration,
        Constant
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public AutoGap(){
        super(Category.Player, "auto-gap", "Automatically eats gapples and egaps if their effects run out.");
    }

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Determines when you eat the gapple.")
            .defaultValue(Mode.Regeneration)
            .build()
    );

    private final Setting<Boolean> preferEgap = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-egap")
            .description("Prefers to eat egapps over regular gapples")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> preferAutoEat = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-auto-eat")
            .description("Whether to use auto-eat or this in the event of a conflict")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableAuras = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-auras")
            .description("disable all auras")
            .defaultValue(false)
            .build()
    );

    @Override
    public void onDeactivate() {
        if(wasThis) {
            ((IKeyBinding) mc.options.keyUse).setPressed(false);
            wasThis = false;
        }
    }

    private int prevSlot;
    private boolean wasKillActive = false;
    private boolean wasCrystalActive = false;
    private boolean wasThis = false;
    private boolean wasAutoEatOn = false;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if(mc.options.keyUse.isPressed() && !wasThis && ModuleManager.INSTANCE.get(AutoEat.class).isActive() && preferAutoEat.get()){
            return;
        }else if(mc.options.keyUse.isPressed() && wasThis && ModuleManager.INSTANCE.get(AutoEat.class).isActive() && !preferAutoEat.get()){
            ModuleManager.INSTANCE.get(AutoEat.class).toggle();
            wasAutoEatOn = true;
        }
        if (mode.get() == Mode.Constant && (mc.player.getMainHandStack().getItem() == Items.GOLDEN_APPLE
                || mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE)) {
            wasThis = true;
            ((IKeyBinding) mc.options.keyUse).setPressed(true);
            return;
        } else if (mode.get() == Mode.Constant &&!(mc.player.getMainHandStack().getItem() == Items.GOLDEN_APPLE
                || mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE)) {
            wasThis = false;
            ((IKeyBinding) mc.options.keyUse).setPressed(false);
            return;
        }
        if(mc.player.getActiveStatusEffects().containsKey(StatusEffects.ABSORPTION) && mc.player.getActiveStatusEffects().containsKey(StatusEffects.REGENERATION)){
            if(mc.options.keyUse.isPressed()){
                ((IKeyBinding) mc.options.keyUse).setPressed(false);
                wasThis = false;
                if(wasAutoEatOn){
                    ModuleManager.INSTANCE.get(AutoEat.class).toggle();
                    wasAutoEatOn = false;
                }
                if(wasKillActive){
                    ModuleManager.INSTANCE.get(KillAura.class).toggle();
                    wasKillActive = false;
                }
                if(wasCrystalActive){
                    ModuleManager.INSTANCE.get(CrystalAura.class).toggle();
                    wasCrystalActive = false;
                }
                mc.player.inventory.selectedSlot = prevSlot;
            }
        } else {
            if(mode.get() == Mode.Fire_Resistance && mc.player.getActiveStatusEffects().containsKey(StatusEffects.FIRE_RESISTANCE)) return;
            int gappleSlot = -1;
            int egapSlot = -1;
            for(int i = 0; i < 9; i++){
                if(mc.player.inventory.getStack(i).getItem() == Items.GOLDEN_APPLE && gappleSlot == -1){
                    gappleSlot = i;
                }else if(mc.player.inventory.getStack(i).getItem() == Items.ENCHANTED_GOLDEN_APPLE && egapSlot == -1){
                    egapSlot = i;
                }
            }
            if (wasThis) {
                if ((mode.get() == Mode.Fire_Resistance || preferEgap.get()) && egapSlot != -1) {
                    mc.player.inventory.selectedSlot = egapSlot;
                } else if (gappleSlot != -1) {
                    mc.player.inventory.selectedSlot = gappleSlot;
                } else if (egapSlot != -1) {
                    mc.player.inventory.selectedSlot = egapSlot;
                }
                ((IKeyBinding) mc.options.keyUse).setPressed(true);
            } else {
                if ((mode.get() == Mode.Fire_Resistance || preferEgap.get()) && egapSlot != -1) {
                    prevSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = egapSlot;
                } else if (gappleSlot != -1) {
                    prevSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = gappleSlot;
                } else if (egapSlot != -1) {
                    prevSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = egapSlot;
                }
                if (disableAuras.get()) {
                    if (ModuleManager.INSTANCE.get(KillAura.class).isActive()) {
                        wasKillActive = true;
                        ModuleManager.INSTANCE.get(KillAura.class).toggle();
                    }
                    if (ModuleManager.INSTANCE.get(CrystalAura.class).isActive()) {
                        wasCrystalActive = true;
                    }
                }
                ((IKeyBinding) mc.options.keyUse).setPressed(true);
                wasThis = true;
            }
        }
    });

    public boolean rightClickThings() {
        return !isActive() || !wasThis;
    }
}
