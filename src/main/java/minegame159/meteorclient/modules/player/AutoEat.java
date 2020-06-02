package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.combat.CrystalAura;
import minegame159.meteorclient.modules.combat.KillAura;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoEat extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private Setting<Boolean> egaps = sgGeneral.add(new BoolSetting.Builder()
            .name("egaps")
            .description("Eat enchanted golden apples.")
            .defaultValue(false)
            .build()
    );

    private Setting<Boolean> gaps = sgGeneral.add(new BoolSetting.Builder()
            .name("gaps")
            .description("Eat golden apples.")
            .defaultValue(false)
            .build()
    );

    private Setting<Boolean> chorus = sgGeneral.add(new BoolSetting.Builder()
            .name("chorus")
            .description("Eat chorus fruit.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> disableAuras = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-auras")
            .description("disable all auras")
            .defaultValue(false)
            .build()
    );

    private boolean wasArmourActive = false;

    private boolean wasKillActive = false;

    private boolean wasCrystalActive = false;

    private boolean isEating;
    private int preSelectedSlot, preFoodLevel;

    public AutoEat() {
        super(Category.Player, "auto-eat", "Automatically eats food.");
    }

    @Override
    public void onDeactivate() {
        if (isEating) {
            ((IKeyBinding) mc.options.keyUse).setPressed(false);
            isEating = false;
            mc.player.inventory.selectedSlot = preSelectedSlot;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
        }
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.player.abilities.creativeMode) return;

        if (isEating) {
            if (mc.overlay == null && (mc.currentScreen == null || mc.currentScreen.passEvents)) {
                if(disableAuras.get()) {
                    if (ModuleManager.INSTANCE.get(AutoArmor.class).isActive()) {
                        wasArmourActive = true;
                        ModuleManager.INSTANCE.get(AutoArmor.class).toggle();
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
                }
                ((IKeyBinding) mc.options.keyUse).setPressed(true);
            } else {
                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            }

            if (mc.player.getHungerManager().getFoodLevel() > preFoodLevel) {
                isEating = false;
                mc.interactionManager.stopUsingItem(mc.player);
                ((IKeyBinding) mc.options.keyUse).setPressed(false);
                if(wasArmourActive) {
                    ModuleManager.INSTANCE.get(AutoArmor.class).toggle();
                    wasArmourActive = false;
                }
                if(wasKillActive){
                    ModuleManager.INSTANCE.get(KillAura.class).toggle();
                    wasKillActive = false;
                }
                if(wasCrystalActive){
                    ModuleManager.INSTANCE.get(CrystalAura.class).toggle();
                    wasCrystalActive = false;
                }
                mc.player.inventory.selectedSlot = preSelectedSlot;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
            }

            return;
        }

        int slot = -1;
        int bestHunger = -1;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getInvStack(i).getItem();
            if (!item.isFood()) continue;

            if (item == Items.ENCHANTED_GOLDEN_APPLE && item.getFoodComponent().getHunger() > bestHunger) {
                if (egaps.get()) {
                    bestHunger = item.getFoodComponent().getHunger();
                    slot = i;
                }
            } else if (item == Items.GOLDEN_APPLE && item.getFoodComponent().getHunger() > bestHunger) {
                if (gaps.get()) {
                    bestHunger = item.getFoodComponent().getHunger();
                    slot = i;
                }
            } else if (item == Items.CHORUS_FRUIT && item.getFoodComponent().getHunger() > bestHunger) {
                if (chorus.get()) {
                    bestHunger = item.getFoodComponent().getHunger();
                    slot = i;
                }
            } else if (item.getFoodComponent().getHunger() > bestHunger) {
                bestHunger = item.getFoodComponent().getHunger();
                slot = i;
            }
        }

        if (slot != -1 && 20 - mc.player.getHungerManager().getFoodLevel() >= bestHunger) {
            preSelectedSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;
            isEating = true;
            preFoodLevel = mc.player.getHungerManager().getFoodLevel();
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }
    });
}
