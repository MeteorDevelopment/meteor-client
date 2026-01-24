/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @ModifyConstant(constant = @Constant(longValue = 80L), method = "collectPlayerEntries")
    private long modifyCount(long count) {
        BetterTab module = Modules.get().get(BetterTab.class);

        return module.isActive() ? module.tabSize.get() : count;
    }

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(PlayerListEntry playerListEntry, CallbackInfoReturnable<Text> info) {
        BetterTab betterTab = Modules.get().get(BetterTab.class);
        NameProtect nameProtect = Modules.get().get(NameProtect.class);

        if (betterTab.isActive()) {
            // Use BetterTab's getPlayerName method which already handles NameProtect and team colors
            Text playerName = betterTab.getPlayerName(playerListEntry);
            info.setReturnValue(playerName);
        } else {
            // For regular tab list, only apply name protection if name changes
            String originalName = playerListEntry.getProfile().name();
            String protectedName = nameProtect.getName(originalName);
            
            if (!originalName.equals(protectedName)) {
                info.setReturnValue(Text.literal(protectedName));
            }
            // If name didn't change, let the original method handle it (keeps team colors)
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    private int modifyWidth(int width) {
        BetterTab module = Modules.get().get(BetterTab.class);

        return module.isActive() && module.accurateLatency.get() ? width + 30 : width;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void modifyHeight(CallbackInfo ci, @Local(ordinal = 5)LocalIntRef o, @Local(ordinal = 6)LocalIntRef p) {
        BetterTab module = Modules.get().get(BetterTab.class);
        if (!module.isActive()) return;

        int newO;
        int newP = 1;
        int totalPlayers = newO = this.collectPlayerEntries().size();
        while (newO > module.tabHeight.get()) {
            newO = (totalPlayers + ++newP - 1) / newP;
        }

        o.set(newO);
        p.set(newP);
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        BetterTab betterTab = Modules.get().get(BetterTab.class);

        if (betterTab.isActive() && betterTab.accurateLatency.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            TextRenderer textRenderer = mc.textRenderer;

            int latency = MathHelper.clamp(entry.getLatency(), 0, 9999);
            int color = latency < 150 ? 0xFF00E970 :
                        latency < 300 ? 0xFFE7D020 : 0xFFD74238;
            String text = String.valueOf(latency);
            context.drawTextWithShadow(textRenderer, text, x + width - textRenderer.getWidth(text), y, color);
            ci.cancel();
        }
    }
}