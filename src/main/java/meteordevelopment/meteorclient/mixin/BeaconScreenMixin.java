/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterBeacons;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        List<Holder<MobEffect>> effects = BeaconBlockEntity.EFFECTS_BY_LEVEL.stream().flatMap(Collection::stream).toList();
        if (Minecraft.getInstance().currentScreen instanceof BeaconScreen beaconScreen) {
            addBeaconButton(beaconScreen.new DoneButtonWidget(this.x + 164, this.y + 107));
            addBeaconButton(beaconScreen.new CancelButtonWidget(this.x + 190, this.y + 107));

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 2; y++) {
                    Holder<MobEffect> effect = effects.get(x * 2 + y);
                    int xMin = this.x + x * 25;
                    int yMin = this.y + y * 25;
                    addBeaconButton(beaconScreen.new EffectButtonWidget(xMin + 27, yMin + 32, effect, true, -1));
                    BeaconScreen.BeaconPowerButton secondaryWidget = beaconScreen.new EffectButtonWidget(xMin + 133, yMin + 32, effect, false, 3);
                    if (getScreenHandler().getProperties() != 4) secondaryWidget.active = false;
                    addBeaconButton(secondaryWidget);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void onDrawBackground(GuiGraphics context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Modules.get().get(BetterBeacons.class).isActive()) return;
        //this will clear the background from useless pyramid graphics
        context.fill(x + 10, y + 7, x + 220, y + 98, 0xFF212121);
    }
}
