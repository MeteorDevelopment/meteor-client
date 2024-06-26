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
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Items;

public class BowSpam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> charge = sgGeneral.add(new IntSetting.Builder()
        .name("charge")
        .description("How long to charge the bow before releasing in ticks.")
        .defaultValue(5)
        .range(5, 20)
        .sliderRange(5, 20)
        .build()
    );

    private final Setting<Boolean> onlyWhenHoldingRightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("when-holding-right-click")
        .description("Works only when holding right click.")
        .defaultValue(false)
        .build()
    );

    private boolean wasBow = false;
    private boolean wasHoldingRightClick = false;

    public BowSpam() {
        super(Categories.Combat, "bow-spam", "Spams arrows.");
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
    private void onTick(TickEvent.Post event) {
        if (!mc.player.getAbilities().creativeMode && !InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found())
            return;

        if (!onlyWhenHoldingRightClick.get() || mc.options.useKey.isPressed()) {
            boolean isBow = mc.player.getMainHandStack().getItem() == Items.BOW;
            if (!isBow && wasBow) setPressed(false);

            wasBow = isBow;
            if (!isBow) return;

            if (mc.player.getItemUseTime() >= charge.get()) {
                mc.player.stopUsingItem();
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
}
