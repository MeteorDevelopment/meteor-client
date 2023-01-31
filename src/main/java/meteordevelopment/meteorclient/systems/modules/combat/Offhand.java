/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.AxeItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Offhand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Item> item = sgGeneral.add(new EnumSetting.Builder<Item>()
        .name("item")
        .description("Which item to hold in your offhand.")
        .defaultValue(Item.Crystal)
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
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> crystalCa = sgGeneral.add(new BoolSetting.Builder()
        .name("crystal-on-ca")
        .description("Holds a crystal when you have Crystal Aura enabled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> crystalMine = sgGeneral.add(new BoolSetting.Builder()
        .name("crystal-on-mine")
        .description("Holds a crystal when you are mining.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fireworkCrossbow = sgGeneral.add(new BoolSetting.Builder()
        .name("firework-with-crossbow")
        .description("Holds the Firework with highest damage when you are holding a Crossbow.")
        .defaultValue(true)
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

        // Sword Gap
        if ((mc.player.getMainHandStack().getItem() instanceof SwordItem
            || mc.player.getMainHandStack().getItem() instanceof AxeItem) && swordGap.get()) currentItem = Item.EGap;

        // Ca and mining
        else if ((Modules.get().isActive(CrystalAura.class) && crystalCa.get())
            || mc.interactionManager.isBreakingBlock() && crystalMine.get()) currentItem = Item.Crystal;

        // Firework with Crossbow
        else if (InvUtils.testInMainHand(Items.CROSSBOW) && fireworkCrossbow.get()) currentItem = Item.Firework;

        else currentItem = item.get();

        // Checking offhand item
        if (mc.player.getOffHandStack().getItem() != currentItem.item) {
            FindItemResult itemResult = findItem();

            // No offhand item
            if (!itemResult.found()) {
                if (!sentMessage) {
                    warning("Chosen item not found.");
                    sentMessage = true;
                }
            }

            // Swap to offhand
            else if ((isClicking || !rightClick.get()) && !autoTotem.isLocked() && !itemResult.isOffhand()) {
                InvUtils.move().from(itemResult.slot()).toOffhand();
                sentMessage = false;
            }
        }

        // If not clicking, switch to totem if auto totem is active or swap offhand item
        else if (!isClicking && rightClick.get()) {
            if (autoTotem.isActive()) {
                FindItemResult totem = InvUtils.find(itemStack -> itemStack.getItem() == Items.TOTEM_OF_UNDYING, hotbar.get() ? 0 : 9, 35);

                if (totem.found() && !totem.isOffhand()) {
                    InvUtils.move().from(totem.slot()).toOffhand();
                }
            } else if (canSwapOffhand()) {
                FindItemResult empty = InvUtils.find(ItemStack::isEmpty, hotbar.get() ? 0 : 9, 35);
                if (empty.found()) InvUtils.move().fromOffhand().to(empty.slot());
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        isClicking = mc.currentScreen == null && !Modules.get().get(AutoTotem.class).isLocked() && !usableItem() && !mc.player.isUsingItem() && event.action == KeyAction.Press && event.button == GLFW_MOUSE_BUTTON_RIGHT;
    }

    private FindItemResult findItem() {
        if (currentItem == Item.Firework) {
            int slot = -1, count = 0;
            int bestDamage = -1;

            for (int i = hotbar.get() ? 0 : 9; i <= 35; i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                if (!itemStack.isOf(Item.Firework.item)) continue;

                NbtCompound compound = itemStack.getSubNbt("Fireworks");
                int damage = compound != null ? compound.getList("Explosions", NbtElement.COMPOUND_TYPE).size() : 0;

                if (damage > bestDamage) {
                    slot = i;
                    count = itemStack.getCount();

                    bestDamage = damage;
                }
            }

            return new FindItemResult(slot, count);
        }

        return InvUtils.find(itemStack -> itemStack.getItem() == currentItem.item , hotbar.get() ? 0 : 9, 35);
    }

    private boolean usableItem() {
        return mc.player.getMainHandStack().isOf(Items.BOW)
            || mc.player.getMainHandStack().isOf(Items.TRIDENT)
            || (mc.player.getMainHandStack().isOf(Items.CROSSBOW) && CrossbowItem.isCharged(mc.player.getMainHandStack()))
            || mc.player.getMainHandStack().getItem().isFood();
    }

    private boolean canSwapOffhand() {
        ItemStack itemStack = mc.player.getMainHandStack();
        return !itemStack.isOf(Items.CROSSBOW) ||
               mc.player.getItemUseTime() < itemStack.getMaxUseTime() || // Project loading
               CrossbowItem.isCharged(itemStack); // Project loaded and ready
    }

    @Override
    public String getInfoString() {
        return item.get().name();
    }

    public enum Item {
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        Gap(Items.GOLDEN_APPLE),
        Crystal(Items.END_CRYSTAL),
        Firework(Items.FIREWORK_ROCKET),
        Totem(Items.TOTEM_OF_UNDYING),
        Shield(Items.SHIELD);

        net.minecraft.item.Item item;

        Item(net.minecraft.item.Item item) {
            this.item = item;
        }
    }
}
