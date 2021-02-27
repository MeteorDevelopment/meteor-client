package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.combat.AnchorAura;
import minegame159.meteorclient.modules.combat.BedAura;
import minegame159.meteorclient.modules.combat.CrystalAura;
import minegame159.meteorclient.modules.combat.KillAura;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoPot extends Module {
    private static final Class<? extends Module>[] AURAS = new Class[] { KillAura.class, CrystalAura.class, AnchorAura.class, BedAura.class };


    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> Healing = sgGeneral.add(new BoolSetting.Builder()
            .name("Healing")
            .description("Enables healing potions.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> Strength = sgGeneral.add(new BoolSetting.Builder()
            .name("Strength")
            .description("Enables strength potions.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> useSplashPots = sgGeneral.add(new BoolSetting.Builder()
            .name("Splash-Pots")
            .description("Allow the use of splash pots")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("If health goes below this point, Healing Pot will trigger.")
            .defaultValue(15)
            .min(0)
            .sliderMax(20)
            .build()
    );
    private final Setting<Boolean> pauseAuras = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-auras")
            .description("Pauses all auras when eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pauseBaritone = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-baritone")
            .description("Pause baritone when eating.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Forces you to rotate downwards when throwing bottles.")
            .defaultValue(true)
            .build()
    );
    private int slot, prevSlot;
    private boolean drinking, splashing;
    private final List<Class<? extends Module>> wasAura = new ArrayList<>();
    private boolean wasBaritone;
    public AutoPot() {
        super(Categories.Player, "auto-pot", "Automatically Drinks Potions");
    }
    //Gilded's first module, lets see how much i'll die making this
    //TODO:Rework everything to accept all pots
    //TODO: Does strength work better if you throw it up? will check.
    @Override
    public void onDeactivate() {
        if (drinking) stopDrinking();
        if (splashing) stopSplashing();
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (Healing.get()) {
            if (ShouldDrinkHealth()) {
                //Heal Pot Slot
                int slot = HealingpotionSlot();
                //Slot Not Invalid
                if (slot != -1) {
                    startDrinking();
                } else if (HealingpotionSlot() == -1 && useSplashPots.get()) {
                    slot = HealingSplashpotionSlot();
                    if (slot != -1) {
                        startSplashing();
                    }
                }
            }
            if (drinking) {
                if (ShouldDrinkHealth()) {
                    if (isNotPotion(mc.player.inventory.getStack(slot))) {
                        slot = HealingpotionSlot();
                        if (slot == -1) {
                            ChatUtils.moduleInfo(this, "Ran out of Pots while drinking");
                            stopDrinking();
                            return;
                        }
                    } else changeSlot(slot);
                }
                drink();
                if (ShouldNotDrinkHealth()) {
                    ChatUtils.moduleInfo(this, "Health Full");
                    stopDrinking();
                    return;
                }
            }
            if (splashing) {
                if (ShouldDrinkHealth()) {
                    if (isNotSplashPotion(mc.player.inventory.getStack(slot))) {
                        slot = HealingSplashpotionSlot();
                        if (slot == -1) {
                            ChatUtils.moduleInfo(this, "Ran out of Pots while splashing");
                            stopSplashing();
                            return;
                        } else changeSlot(slot);
                    }
                    splash();
                    if (ShouldNotDrinkHealth()) {
                        ChatUtils.moduleInfo(this, "Health Full");
                        stopSplashing();
                        return;
                    }
                }
            }
        }
        if (Strength.get()) {
            if (ShouldDrinkStrength()) {
                //Strength Pot Slot
                int slot = StrengthpotionSlot();
                //Slot Not Invalid
                if (slot != -1) {
                    startDrinking();
                }
                else if (StrengthpotionSlot() == -1 && useSplashPots.get()) {
                    slot = StrengthSplashpotionSlot();
                    if (slot != -1) {
                        startSplashing();
                    }
                }
            }
            if (drinking) {
                if (ShouldDrinkStrength()) {
                    if (isNotPotion(mc.player.inventory.getStack(slot))) {
                        slot = StrengthpotionSlot();
                        if (slot == -1) {
                            stopDrinking();
                            ChatUtils.moduleInfo(this, "Out of Pots");
                            return;
                        } else changeSlot(slot);
                    }
                    drink();
                } else {
                    stopDrinking();
                }
            }
            if (splashing) {
                if (ShouldDrinkStrength()) {
                    if (isNotSplashPotion(mc.player.inventory.getStack(slot))) {
                        slot = StrengthSplashpotionSlot();
                        if (slot == -1) {
                            ChatUtils.moduleInfo(this, "Ran out of Pots while splashing");
                            stopSplashing();
                            return;
                        } else changeSlot(slot);
                    }
                    splash();
                } else {
                    stopSplashing();
                }
            }
        }
    }
    @EventHandler
    private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
        if (drinking) event.target = null;
    }
    private void setPressed(boolean pressed) {
        mc.options.keyUse.setPressed(pressed);
    }
private void startDrinking() {
        prevSlot = mc.player.inventory.selectedSlot;
        drink();
        // Pause auras
        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }
        // Pause baritone
        wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }
    }
    private void startSplashing() {
        prevSlot = mc.player.inventory.selectedSlot;
        if (lookDown.get()){
            Rotations.rotate(mc.player.yaw, 90); splash();
        }
        splash();
        // Pause auras
        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }
        // Pause baritone
        wasBaritone = false;
        if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            wasBaritone = true;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
        }
    }
private void drink() {
        changeSlot(slot);
        setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();

        drinking = true;
    }
    private void splash() {
        changeSlot(slot);
        setPressed(true);
        splashing = true;
    }
    private void stopDrinking() {
        changeSlot(prevSlot);
        setPressed(false);
        drinking = false;

        // Resume auras
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }
        // Resume baritone
        if (pauseBaritone.get() && wasBaritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
        }
    }
    private void stopSplashing() {
        changeSlot(prevSlot);
        setPressed(false);

        splashing = false;

        // Resume auras
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }
        // Resume baritone
        if (pauseBaritone.get() && wasBaritone) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
        }
    }
    private double truehealth() {
        assert mc.player != null;
        return mc.player.getHealth();
    }
private void changeSlot(int slot) {
        mc.player.inventory.selectedSlot = slot;
        this.slot = slot;
    }
//Sunk 7 hours into these checks, if i die blame checks
    //Heal pot checks
    private int HealingpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.inventory.getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.POTION) continue;
            if (stack.getItem() == Items.POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.instant_health")) {
                        slot = i;
                        break;
                    }
                }
            }
        }
        return slot;
    }
     private int HealingSplashpotionSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.inventory.getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.SPLASH_POTION) continue;
            if (stack.getItem() == Items.SPLASH_POTION) {
                List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects();
                if (effects.size() > 0) {
                    StatusEffectInstance effect = effects.get(0);
                    if (effect.getTranslationKey().equals("effect.minecraft.instant_health")) {
                        slot = i;
                        break;
                    }
                }
            }
        }
        return slot;
    }
    //Strength Pot Checks
            private int StrengthSplashpotionSlot () {
                int slot = -1;
                for (int i = 0; i < 9; i++) {
                    // Skip if item stack is empty
                    ItemStack stack = mc.player.inventory.getStack(i);
                    if (stack.isEmpty()) continue;
                    if (stack.getItem() != Items.SPLASH_POTION) continue;
                    if (stack.getItem() == Items.SPLASH_POTION) {
                        List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects();
                        if (effects.size() > 0) {
                            StatusEffectInstance effect = effects.get(0);
                            if (effect.getTranslationKey().equals("effect.minecraft.strength")) {
                                slot = i;
                                break;
                            }
                        }

                    }
                }
                return slot;
            }
            private int StrengthpotionSlot () {
                int slot = -1;
                for (int i = 0; i < 9; i++) {
                    // Skip if item stack is empty
                    ItemStack stack = mc.player.inventory.getStack(i);
                    if (stack.isEmpty()) continue;
                    if (stack.getItem() != Items.POTION) continue;
                    if (stack.getItem() == Items.POTION) {
                        List<StatusEffectInstance> effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects();
                        if (effects.size() > 0) {
                            StatusEffectInstance effect = effects.get(0);
                            if (effect.getTranslationKey().equals("effect.minecraft.strength")) {
                                slot = i;
                                break;
                            }
                        }

                    }
                }
                return slot;
            }
private boolean isNotPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.POTION;
    }
    private boolean isNotSplashPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.SPLASH_POTION;
    }
    private boolean ShouldDrinkHealth(){
         if (truehealth() < health.get()) return true;
         return false;
    }
private boolean ShouldNotDrinkHealth(){
         if (truehealth() >= health.get()) return true;
         return false;
    }
private boolean ShouldDrinkStrength(){
        Map<StatusEffect, StatusEffectInstance> effects = mc.player.getActiveStatusEffects();
    return !effects.containsKey(StatusEffects.STRENGTH);
}
        }