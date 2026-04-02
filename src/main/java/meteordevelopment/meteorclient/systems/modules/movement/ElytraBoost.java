/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;

import java.util.ArrayList;
import java.util.List;

public class ElytraBoost extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> dontConsumeFirework = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-consume")
        .description("Prevents fireworks from being consumed when using Elytra Boost.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fireworkLevel = sgGeneral.add(new IntSetting.Builder()
        .name("firework-duration")
        .description("The duration of the firework.")
        .defaultValue(0)
        .range(0, 255)
        .sliderMax(255)
        .build()
    );

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
        .name("play-sound")
        .description("Plays the firework sound when a boost is triggered.")
        .defaultValue(true)
        .build()
    );

    @SuppressWarnings("unused")
    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The keybind to boost.")
        .action(this::boost)
        .build()
    );

    private final List<FireworkRocketEntity> fireworks = new ArrayList<>();

    public ElytraBoost() {
        super(Categories.Movement, "elytra-boost", "Boosts your elytra as if you used a firework.");
    }

    @Override
    public void onDeactivate() {
        fireworks.clear();
    }

    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        ItemStack itemStack = mc.player.getItemInHand(event.hand);

        if (itemStack.getItem() instanceof FireworkRocketItem && dontConsumeFirework.get()) {
            event.toReturn = InteractionResult.PASS;

            boost();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        fireworks.removeIf(Entity::isRemoved);
    }

    private void boost() {
        if (!Utils.canUpdate()) return;

        if (mc.player.isFallFlying() && mc.screen == null) {
            ItemStack itemStack = Items.FIREWORK_ROCKET.getDefaultInstance();
            itemStack.set(DataComponents.FIREWORKS, new Fireworks(fireworkLevel.get(), itemStack.get(DataComponents.FIREWORKS).explosions()));

            FireworkRocketEntity entity = new FireworkRocketEntity(mc.level, itemStack, mc.player);
            fireworks.add(entity);
            if (playSound.get())
                mc.level.playSound(mc.player, entity, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F);
            mc.level.addEntity(entity);
        }
    }

    public boolean isFirework(FireworkRocketEntity firework) {
        return isActive() && fireworks.contains(firework);
    }
}
