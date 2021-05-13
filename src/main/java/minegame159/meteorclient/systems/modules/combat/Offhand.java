/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.meteor.MouseButtonEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.SlotUtils;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;

import java.util.function.Predicate;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Offhand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Item> item = sgGeneral.add(new EnumSetting.Builder<Item>()
            .name("item")
            .description("Which item to hold in your offhand.")
            .defaultValue(Item.EGap)
            .build()
    );

    private final Setting<Boolean> hotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("hotbar")
            .description("Whether to use items from your hotbar.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rightClick = sgGeneral.add(new BoolSetting.Builder()
            .name("right-click")
            .description("Only holds the item in your offhand when you are holding right click.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> swordGap = sgGeneral.add(new BoolSetting.Builder()
            .name("sword-gap")
            .description("Holds an Enchanted Golden Apple when you are holding a sword.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> crystalCa = sgGeneral.add(new BoolSetting.Builder()
            .name("crystal-on-ca")
            .description("Holds a crystal when you have Crystal Aura enabled.")
            .defaultValue(false)
            .build()
    );

    private boolean isClicking;
    private boolean sentMessage;
    private Item currentItem;

    public Offhand() {
        super(Categories.Combat, "offhand", "Allows you to hold specified items in your offhand.");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
        isClicking = false;
        currentItem = item.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        AutoTotem autoTotem = Modules.get().get(AutoTotem.class);

        // Sword Gap and CA checks

        if ((mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem) && swordGap.get()) currentItem = Item.EGap;
        else if (Modules.get().isActive(CrystalAura.class) && crystalCa.get()) currentItem = Item.Crystal;
        else currentItem = item.get();

        // Checking offhand item
        if (mc.player.getOffHandStack().getItem() != currentItem.item) {
            int slot = getSlot(currentItem.item);

            // No offhand item
            if (slot == -1) {
                if (!sentMessage) {
                    ChatUtils.moduleWarning(this, "Chosen item not found.");
                    sentMessage = true;
                }
            }

            // Swap to offhand
            else if ((isClicking || !rightClick.get()) && !autoTotem.isLocked()) {
                InvUtils.move().from(slot).toOffhand();
                sentMessage = false;
            }
        }

        // If not clicking, set to totem if auto totem is on
        else if ((!isClicking && rightClick.get())) {
            int slot;

            if (autoTotem.isActive()) {
                slot = getSlot(Items.TOTEM_OF_UNDYING);
                if (slot != -1) InvUtils.move().from(slot).toOffhand();
            } else {
                slot = getSlot(ItemStack::isEmpty);
                if (slot != -1) InvUtils.move().from(SlotUtils.OFFHAND).to(slot);
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

    private int getSlot(net.minecraft.item.Item item) {
        return getSlot(itemStack -> itemStack.getItem() == item);
    }

    private int getSlot(Predicate<ItemStack> item) {
        if (hotbar.get()) return InvUtils.findItemInWhole(item);
        else return InvUtils.findItemInInventory(item);
    }

    @Override
    public String getInfoString() {
        return item.get().name();
    }

    public enum Item {
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        Gap(Items.GOLDEN_APPLE),
        Crystal(Items.END_CRYSTAL),
        Shield(Items.SHIELD);

        net.minecraft.item.Item item;

        Item(net.minecraft.item.Item item) {
            this.item = item;
        }
    }
}
