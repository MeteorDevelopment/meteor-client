/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;

import static meteordevelopment.orbit.EventPriority.HIGHEST;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Offhand extends Module {
    private final SettingGroup sgCombat = settings.createGroup("Combat");
    private final SettingGroup sgTotem = settings.createGroup("Totem");

    //Combat

    private final Setting<Integer> delayTicks = sgCombat.add(new IntSetting.Builder()
        .name("item-switch-delay")
        .description("The delay in ticks between slot movements.")
        .defaultValue(1)
        .min(1)
        .sliderMax(20)
        .build()
    );
    private final Setting<Item> preferredItem = sgCombat.add(new EnumSetting.Builder<Item>()
        .name("item")
        .description("Which item to hold in your offhand.")
        .defaultValue(Item.Crystal)
        .build()
    );

    private final Setting<Boolean> hotbar = sgCombat.add(new BoolSetting.Builder()
        .name("hotbar")
        .description("Whether to use items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rightGapple = sgCombat.add(new BoolSetting.Builder()
        .name("right-gapple")
        .description("Will switch to a gapple when holding right click.")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> swordGap = sgCombat.add(new BoolSetting.Builder()
        .name("sword-gapple")
        .description("Will switch to a gapple when holding right click.")
        .defaultValue(true)
        .visible(rightGapple::get)
        .build()
    );

    //Totem

    private final Setting<Double> minHealth = sgTotem.add(new DoubleSetting.Builder()
        .name("min-health")
        .description("Will hold a totem when below this amount of health.")
        .defaultValue(10)
        .range(0,36)
        .sliderRange(0,36)
        .build()
    );

    private final Setting<Boolean> elytra = sgTotem.add(new BoolSetting.Builder()
        .name("elytra")
        .description("Will always hold a totem while flying with an elytra.")
        .defaultValue(false)
        .build()
    );


    private final Setting<Boolean> falling = sgTotem.add(new BoolSetting.Builder()
        .name("falling")
        .description("Will hold a totem if fall damage could kill you.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> explosion = sgTotem.add(new BoolSetting.Builder()
        .name("explosion")
        .description("Will hold a totem when explosion damage could kill you.")
        .defaultValue(true)
        .build()
    );


    private boolean isClicking;
    private boolean sentMessage;

    private Item currentItem;
    public boolean locked;


    private int totems, ticks;

    public Offhand() {
        super(Categories.Combat, "off-hand", "Allows you to hold specified items in your offhand.");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
        isClicking = false;
        currentItem = preferredItem.get();
    }

    @EventHandler(priority = HIGHEST + 999)
    private void onTick(TickEvent.Pre event) throws InterruptedException {
        FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
        totems = result.count();

        if (totems <= 0) locked = false;
        else if (ticks >= delayTicks.get()) {
            boolean low = mc.player.getHealth() + mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions(explosion.get(), falling.get()) <= minHealth.get();
            boolean ely = elytra.get() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && mc.player.isFallFlying();
            FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == currentItem.item, 0, 35);

            locked = (low || ely);

            if (locked && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                InvUtils.move().from(result.slot()).toOffhand();
            }

            ticks = 0;
            return;
        }
        ticks++;

        AutoTotem autoTotem = Modules.get().get(AutoTotem.class);
        currentItem = preferredItem.get();

        // swordGap
        if (rightGapple.get()) {
            if (!locked) {
                if (swordGap.get() && mc.player.getMainHandStack().getItem() instanceof SwordItem) {
                    if (isClicking) {
                        currentItem = Item.EGap;
                    }
                }
                if (!swordGap.get()) {
                    if (isClicking) {
                        currentItem = Item.EGap;
                    }
                }
            }
        }

        else currentItem = preferredItem.get();

        // Checking off-hand item
        if (mc.player.getOffHandStack().getItem() != currentItem.item) {
            if (ticks >= delayTicks.get()) {
                if (!locked) {
                    FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == currentItem.item, hotbar.get() ? 0 : 9, 35);

                    // No offhand item
                    if (!item.found()) {
                        if (!sentMessage) {
                            warning("Chosen item not found.");
                            sentMessage = true;
                        }
                    }

                    // Swap to offhand
                    else if ((isClicking || !autoTotem.isLocked() && !item.isOffhand())) {
                        InvUtils.move().from(item.slot()).toOffhand();
                        sentMessage = false;
                    }
                    ticks = 0;
                    return;
                }
                ticks++;
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        isClicking = mc.currentScreen == null && !Modules.get().get(AutoTotem.class).isLocked() && !usableItem() && !mc.player.isUsingItem() && event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_RIGHT;
    }

    private boolean usableItem() {
        return mc.player.getMainHandStack().getItem() == Items.BOW
            || mc.player.getMainHandStack().getItem() == Items.TRIDENT
            || mc.player.getMainHandStack().getItem() == Items.CROSSBOW
            || mc.player.getMainHandStack().getItem().isFood();
    }

    @Override
    public String getInfoString() {
        return preferredItem.get().name();
    }

    public enum Item {
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        Gap(Items.GOLDEN_APPLE),
        Crystal(Items.END_CRYSTAL),
        Totem(Items.TOTEM_OF_UNDYING),
        Shield(Items.SHIELD);

        net.minecraft.item.Item item;
        Item(net.minecraft.item.Item item) {
            this.item = item;
        }

    }

}

