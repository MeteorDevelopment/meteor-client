/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.HitResult;

public class AutoClicker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> leftClickMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode-left")
        .description("The method of clicking for left clicks.")
        .defaultValue(Mode.Press)
        .build()
    );

    private final Setting<Integer> leftClickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-left")
        .description("The amount of delay between left clicks in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(60)
        .visible(() -> leftClickMode.get() == Mode.Press)
        .build()
    );


    private final Setting<Boolean> smartDelay = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-delay")
        .description("Uses the vanilla cooldown to attack entities.")
        .defaultValue(true)
        .visible(() -> leftClickMode.get() == Mode.Press)
        .build()
    );

    private final Setting<Boolean> breakBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("break-blocks")
        .description("Allow breaking blocks when autoclicking.")
        .defaultValue(true)
        .visible(() -> leftClickMode.get() == Mode.Press)
        .build()
    );

    private final Setting<Boolean> onlyWhenHoldingLeftClick = sgGeneral.add(new BoolSetting.Builder()
        .name("when-holding-left-click")
        .description("Works only when holding left click.")
        .defaultValue(true)
        .visible(() -> leftClickMode.get() == Mode.Press)
        .build()
    );

    private final Setting<Mode> rightClickMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode-right")
        .description("The method of clicking for right clicks.")
        .defaultValue(Mode.Press)
        .build()
    );

    private final Setting<Integer> rightClickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-right")
        .description("The amount of delay between right clicks in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(60)
        .visible(() -> rightClickMode.get() == Mode.Press)
        .build()
    );

    private final Setting<Boolean> onlyWhenHoldingRightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("when-holding-right-click")
        .description("Works only when holding right click.")
        .defaultValue(true)
        .visible(() -> rightClickMode.get() == Mode.Press)
        .build()
    );

    private int rightClickTimer, leftClickTimer;

    public AutoClicker() {
        super(Categories.Player, "auto-clicker", "Automatically clicks.");
    }

    @Override
    public void onActivate() {
        rightClickTimer = 0;
        leftClickTimer = 0;
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }

    @Override
    public void onDeactivate() {
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        switch (leftClickMode.get()) {
            case Disabled -> {}
            case Hold -> mc.options.attackKey.setPressed(true);
            case Press -> {
                leftClickTimer++;
                if (mc.currentScreen != null) break;
                boolean block = mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
                if (breakBlocks.get() && block && Input.isPressedButton(mc.options.attackKey)) {
                    mc.options.attackKey.setPressed(true);
                    leftClickTimer = 0;
                    break;
                }
                if (smartDelay.get() ? mc.player.getAttackCooldownProgress(0) >= 1 : leftClickTimer > leftClickDelay.get())
                    if (!onlyWhenHoldingLeftClick.get() || Input.isPressedButton(mc.options.attackKey)) {
                        if (!breakBlocks.get() || !block) {
                            Utils.leftClick();
                            leftClickTimer = 0;
                        }
                    }
            }
        }
        switch (rightClickMode.get()) {
            case Disabled -> {}
            case Hold -> mc.options.useKey.setPressed(true);
            case Press -> {
                rightClickTimer++;
                if (mc.currentScreen != null) break;
                if ((!onlyWhenHoldingRightClick.get() || Input.isPressedButton(mc.options.useKey))
                    && rightClickTimer > rightClickDelay.get()) {
                    Utils.rightClick();
                    rightClickTimer = 0;
                }
            }
        }
    }

    public enum Mode {
        Disabled,
        Hold,
        Press
    }
}
