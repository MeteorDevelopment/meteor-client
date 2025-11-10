/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class BowSpam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCrossbows = settings.createGroup("Crossbows");

    private final Setting<Integer> charge = sgGeneral.add(new IntSetting.Builder()
        .name("charge")
        .description("How long to charge the bow before releasing in ticks.")
        .defaultValue(5)
        .range(4, 20)
        .sliderRange(4, 20)
        .build()
    );

    private final Setting<Boolean> onlyWhenHoldingRightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("when-holding-right-click")
        .description("Works only when holding right click.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> spamCrossbows = sgCrossbows.add(new BoolSetting.Builder()
        .name("spam-crossbows")
        .description("Whether to spam loaded crossbows; takes priority over charging bows.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> crossbowDelay = sgCrossbows.add(new IntSetting.Builder()
        .name("crossbow-delay")
        .description("Delay between shooting crossbows in ticks.")
        .defaultValue(10)
        .sliderRange(0, 20)
        .min(0)
        .build()
    );

    private final Setting<Boolean> searchInventory = sgCrossbows.add(new BoolSetting.Builder()
        .name("search-inventory")
        .description("Whether to search your inventory to find loaded crossbows.")
        .defaultValue(true)
        .build()
    );

    private boolean wasBow = false;
    private boolean wasHoldingRightClick = false;
    private int ticks = 0;

    public BowSpam() {
        super(Categories.Combat, "bow-spam", "Spams bows and crossbows.", "auto-bow", "crossbow-spam", "auto-crossbow");
    }

    @Override
    public void onActivate() {
        wasBow = false;
        wasHoldingRightClick = false;
    }

    @Override
    public void onDeactivate() {
        setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult crossbow = searchInventory.get() ? InvUtils.find(this::crossbow) : InvUtils.find(this::crossbow, 0, 8);
        if (spamCrossbows.get() && crossbow.found()) {
            if (ticks >= crossbowDelay.get()) {
                int slot = crossbow.slot();
                if (!crossbow.isHotbar()) {
                    FindItemResult valid = InvUtils.find(stack -> stack.isEmpty() || stack.isOf(Items.CROSSBOW) || stack.isOf(Items.ARROW), 0, 8);
                    if (!valid.found()) return;

                    InvUtils.quickSwap().fromId(valid.slot()).to(crossbow.slot());
                    slot = valid.slot();
                }

                InvUtils.swap(slot, true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();

                ticks = 0;
            }
            else {
                ticks++;
            }

            return;
        }

        if (!mc.player.getAbilities().creativeMode && !InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found())
            return;

        if (!onlyWhenHoldingRightClick.get() || mc.options.useKey.isPressed()) {
            boolean isBow = InvUtils.testInHands(Items.BOW);
            if (!isBow && wasBow) setPressed(false);

            wasBow = isBow;
            if (!isBow) return;

            if (mc.player.getItemUseTime() >= charge.get()) {
                mc.interactionManager.stopUsingItem(mc.player);
            } else {
                setPressed(true);
            }

            wasHoldingRightClick = mc.options.useKey.isPressed();
        } else {
            if (wasHoldingRightClick) {
                setPressed(false);
                wasHoldingRightClick = false;
            }
        }
    }

    private void setPressed(boolean pressed) {
        mc.options.useKey.setPressed(pressed);
    }

    private boolean crossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
    }
}
