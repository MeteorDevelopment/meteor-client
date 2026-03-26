/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterBeacons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreen<BeaconMenu> {
    @Shadow
    protected abstract <T extends AbstractWidget> void addBeaconButton(T button);

    public BeaconScreenMixin(BeaconMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift = At.Shift.AFTER), cancellable = true)
    private void changeButtons(CallbackInfo ci) {
        if (!Modules.get().get(BetterBeacons.class).isActive()) return;
        List<Holder<MobEffect>> effects = BeaconBlockEntity.BEACON_EFFECTS.stream().flatMap(Collection::stream).toList();
        if (Minecraft.getInstance().screen instanceof BeaconScreen beaconScreen) {
            addBeaconButton(newBeaconButton(beaconScreen, "BeaconConfirmButton", new Class<?>[] { int.class, int.class }, this.leftPos + 164, this.topPos + 107));
            addBeaconButton(newBeaconButton(beaconScreen, "BeaconCancelButton", new Class<?>[] { int.class, int.class }, this.leftPos + 190, this.topPos + 107));

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 2; y++) {
                    Holder<MobEffect> effect = effects.get(x * 2 + y);
                    int xMin = this.leftPos + x * 25;
                    int yMin = this.topPos + y * 25;
                    addBeaconButton(newBeaconButton(beaconScreen, "BeaconPowerButton", new Class<?>[] { int.class, int.class, Holder.class, boolean.class, int.class }, xMin + 27, yMin + 32, effect, true, -1));
                    AbstractWidget secondaryWidget = newBeaconButton(beaconScreen, "BeaconPowerButton", new Class<?>[] { int.class, int.class, Holder.class, boolean.class, int.class }, xMin + 133, yMin + 32, effect, false, 3);
                    if (getMenu().getLevels() != 4) secondaryWidget.active = false;
                    addBeaconButton(secondaryWidget);
                }
            }
        }
        ci.cancel();
    }

    @SuppressWarnings("unchecked")
    private static AbstractWidget newBeaconButton(BeaconScreen beaconScreen, String className, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> buttonClass = Class.forName("net.minecraft.client.gui.screens.inventory.BeaconScreen$" + className);
            Class<?>[] fullParameterTypes = new Class<?>[parameterTypes.length + 1];
            Object[] fullArgs = new Object[args.length + 1];
            fullParameterTypes[0] = BeaconScreen.class;
            fullArgs[0] = beaconScreen;
            System.arraycopy(parameterTypes, 0, fullParameterTypes, 1, parameterTypes.length);
            System.arraycopy(args, 0, fullArgs, 1, args.length);

            Constructor<?> constructor = buttonClass.getDeclaredConstructor(fullParameterTypes);
            constructor.setAccessible(true);
            return (AbstractWidget) constructor.newInstance(fullArgs);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create beacon button " + className, e);
        }
    }
}
