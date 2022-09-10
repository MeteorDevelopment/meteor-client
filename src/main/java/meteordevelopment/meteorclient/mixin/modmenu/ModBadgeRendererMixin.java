/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.modmenu;

import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModBadgeRenderer;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModBadgeRenderer.class, remap = false)
public abstract class ModBadgeRendererMixin {
    @Shadow protected Mod mod;
    @Shadow public abstract void drawBadge(MatrixStack matrices, OrderedText text, int outlineColor, int fillColor, int mouseX, int mouseY);

    @Inject(method = "draw", at = @At("TAIL"))
    private void injectDraw(MatrixStack matrices, int mouseX, int mouseY, CallbackInfo ci) {
        Mod mod = this.mod;
        if (!mod.isReal()) return;
        LoaderModMetadata meta = (LoaderModMetadata) ((FabricModAccessor) mod).getMetadata();
        if (meta.getId().equals("meteor-client") || meta.getEntrypointKeys().contains("meteor"))
            drawBadge(matrices, Text.literal("Meteor").asOrderedText(), 0xFF913DE2, 0x99913DE2, mouseX, mouseY);
    }
}
