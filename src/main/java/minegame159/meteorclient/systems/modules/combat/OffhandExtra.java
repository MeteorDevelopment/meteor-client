/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.meteor.MouseButtonEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.*;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class OffhandExtra extends Module {
    public enum Mode{
        EGap,
        Gap,
        EXP,
        Crystal,
        Shield
    }
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExtra = settings.createGroup("Extras");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Changes what item that will go into your offhand.")
            .defaultValue(Mode.EGap)
            .onChanged(mode -> currentMode = mode)
            .build()
    );

    private final Setting<Boolean> replace = sgGeneral.add(new BoolSetting.Builder()
            .name("replace")
            .description("Replaces your offhand or waits until your offhand is empty.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> asimov = sgGeneral.add(new BoolSetting.Builder()
            .name("asimov")
            .description("Always holds the item specified in your offhand.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("The health at which Offhand Extra stops working.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Toggles when you run out of the item you chose.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> hotBar = sgGeneral.add(new BoolSetting.Builder()
            .name("search-hotbar")
            .description("Whether to take items out of your hotbar or not.")
            .defaultValue(false)
            .build()
    );

    // Extras

    private final Setting<Boolean> sword = sgExtra.add(new BoolSetting.Builder()
            .name("sword-gap")
            .description("Changes the mode to EGap if you are holding a sword in your main hand.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> offhandCrystal = sgExtra.add(new BoolSetting.Builder()
            .name("offhand-crystal-on-gap")
            .description("Changes the mode to Crystal if you are holding an enchanted golden apple in your main hand.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> offhandCA = sgExtra.add(new BoolSetting.Builder()
            .name("offhand-crystal-on-ca")
            .description("Changes the mode to Crystal when Crystal Aura is on.")
            .defaultValue(false)
            .build()
    );

    public OffhandExtra() {
        super(Categories.Combat, "offhand-extra", "Allows you to use specified items in your offhand. REQUIRES AutoTotem to be on smart mode.");
    }

    private boolean isClicking = false;
    private boolean sentMessage = false;
    private boolean noTotems = false;
    private Mode currentMode = mode.get();

    @Override
    public void onActivate() {
        currentMode = mode.get();
    }

    @Override
    public void onDeactivate() {
        if (mc.world == null || mc.player == null) return;
        if (Modules.get().isActive(AutoTotem.class) && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.FindItemResult result = InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING);
            if (result.slot != -1) {
                InvUtils.addSlots(1, 45, InvUtils.invIndexToSlotId(result.slot), 1);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        assert mc.player != null;
        currentMode = mode.get();

        if (mc.currentScreen != null && ((!(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof WidgetScreen)) || !asimov.get())) return;
        if (!mc.player.isUsingItem()) isClicking = false;
        if (Modules.get().get(AutoTotem.class).getLocked()) return;

        if ((mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem) && sword.get()) currentMode = Mode.EGap;
        else if (mc.player.getMainHandStack().getItem() instanceof EnchantedGoldenAppleItem && offhandCrystal.get()) currentMode = Mode.Crystal;
        else if (Modules.get().isActive(CrystalAura.class) && offhandCA.get()) currentMode = Mode.Crystal;

        if ((asimov.get() || noTotems) && mc.player.getOffHandStack().getItem() != getItem()) {
            int result = findSlot(getItem());
            if (result == -1 && mc.player.getOffHandStack().getItem() != getItem()) {
                if (currentMode != mode.get()){
                    currentMode = mode.get();
                    if (mc.player.getOffHandStack().getItem() != getItem()) {
                        result = findSlot(getItem());
                        if (result != -1) {
                            InvUtils.addSlots(1, 45, InvUtils.invIndexToSlotId(result), 1);
                            return;
                        }
                    }
                }
                if (!sentMessage) {
                    ChatUtils.moduleWarning(this, "Chosen item not found." + (selfToggle.get() ? " Disabling." : ""));
                    sentMessage = true;
                }
                if (selfToggle.get()) this.toggle();
                return;
            }
            if (mc.player.getOffHandStack().getItem() != getItem() && replace.get()) {
                InvUtils.addSlots(1, 45, InvUtils.invIndexToSlotId(result), 1);
                sentMessage = false;
            }
        } else if (!asimov.get() && !isClicking && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            int result = findSlot(Items.TOTEM_OF_UNDYING);
            if (result != -1) {
                InvUtils.addSlots(1, 45, InvUtils.invIndexToSlotId(result), 1);
            }

        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != GLFW_MOUSE_BUTTON_RIGHT) return;

        if (mc.currentScreen != null) return;
        if (Modules.get().get(AutoTotem.class).getLocked() || !canMove()) return;
        if ((mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || (mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.get())
                && (mc.player.getOffHandStack().getItem() != getItem()) && !(mc.currentScreen instanceof HandledScreen<?>))) {
            if (mc.player.getMainHandStack().getItem() instanceof SwordItem && sword.get()) currentMode = Mode.EGap;
            else if (mc.player.getMainHandStack().getItem() instanceof EnchantedGoldenAppleItem && offhandCrystal.get()) currentMode = Mode.Crystal;
            else if (Modules.get().isActive(CrystalAura.class) && offhandCA.get()) currentMode = Mode.Crystal;
            if (mc.player.getOffHandStack().getItem() == getItem()) return;
            isClicking = true;
            Item item = getItem();
            int result = findSlot(item);
            if (result == -1 && mc.player.getOffHandStack().getItem() != getItem()) {
                if (!sentMessage) {
                    ChatUtils.moduleWarning(this, "Chosen item not found." + (selfToggle.get() ? " Disabling." : ""));
                    sentMessage = true;
                }
                if (selfToggle.get()) this.toggle();
                return;
            }
            if (mc.player.getOffHandStack().getItem() != item && mc.player.getMainHandStack().getItem() != item && replace.get()) {
                InvUtils.addSlots(1, 45, InvUtils.invIndexToSlotId(result), 1);
                sentMessage = false;
            }
            currentMode = mode.get();
        }
    }

    private Item getItem(){
        Item item = Items.TOTEM_OF_UNDYING;
        switch (currentMode) {
            case EGap:
                item = Items.ENCHANTED_GOLDEN_APPLE;
                break;
            case EXP:
                item = Items.EXPERIENCE_BOTTLE;
                break;
            case Gap:
                item = Items.GOLDEN_APPLE;
                break;
            case Crystal:
                item = Items.END_CRYSTAL;
                break;
            case Shield:
                item = Items.SHIELD;
                break;
        }
        return item;
    }

    public void setTotems(boolean set) {
        noTotems = set;
    }

    private boolean canMove(){
        assert mc.player != null;
        return mc.player.getMainHandStack().getItem() != Items.BOW
                && mc.player.getMainHandStack().getItem() != Items.TRIDENT
                && mc.player.getMainHandStack().getItem() != Items.CROSSBOW
                && !mc.player.getMainHandStack().getItem().isFood();
    }

    private int findSlot(Item item){
        assert mc.player != null;
        for (int i = 9; i < mc.player.inventory.size(); i++){
            if (mc.player.inventory.getStack(i).getItem() == item){
                return i;
            }
        }
        if (hotBar.get()) return InvUtils.findItemWithCount(item).slot;
        return -1;
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }
}
