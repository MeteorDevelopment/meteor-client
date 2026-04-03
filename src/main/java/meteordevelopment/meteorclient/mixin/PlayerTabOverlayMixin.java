/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Shadow
    protected abstract List<PlayerInfo> getPlayerInfos();

    @ModifyConstant(constant = @Constant(longValue = 80L), method = "getPlayerInfos")
    private long modifyCount(long count) {
        BetterTab module = Modules.get().get(BetterTab.class);

        return module.isActive() ? module.tabSize.get() : count;
    }

    @Inject(method = "getNameForDisplay", at = @At("HEAD"), cancellable = true)
    public void getNameForDisplay(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        BetterTab betterTab = Modules.get().get(BetterTab.class);

        if (betterTab.isActive()) cir.setReturnValue(betterTab.getPlayerName(info));
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    private int modifyWidth(int width) {
        BetterTab module = Modules.get().get(BetterTab.class);

        return module.isActive() && module.accurateLatency.get() ? width + 30 : width;
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void modifyHeight(CallbackInfo ci, @Local(name = "rows") LocalIntRef rows, @Local(name = "cols") LocalIntRef cols) {
        BetterTab module = Modules.get().get(BetterTab.class);
        if (!module.isActive()) return;

        int newRows;
        int newCols = 1;
        int totalPlayers = newRows = this.getPlayerInfos().size();
        while (newRows > module.tabHeight.get()) {
            newRows = (totalPlayers + ++newCols - 1) / newCols;
        }

        rows.set(newRows);
        cols.set(newCols);
    }

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void onExtractPingIcon(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo ci) {
        BetterTab betterTab = Modules.get().get(BetterTab.class);

        if (betterTab.isActive() && betterTab.accurateLatency.get()) {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;

            int latency = Mth.clamp(info.getLatency(), 0, 9999);
            int color = latency < 150 ? 0xFF00E970 :
                latency < 300 ? 0xFFE7D020 : 0xFFD74238;
            String text = latency + "ms";
            graphics.text(font, text, xo + slotWidth - font.width(text), yo, color);
            ci.cancel();
        }
    }
}
