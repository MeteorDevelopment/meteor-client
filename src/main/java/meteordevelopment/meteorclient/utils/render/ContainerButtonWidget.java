/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class ContainerButtonWidget extends ButtonWidget {
    public ContainerButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        TextRenderer textRenderer = minecraftClient.textRenderer;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int halfWidth = width / 2;
        int halfHeight = height / 2;
        int texY = getYImage(isHovered()) * 20;

        drawTexture(matrices, x, y, 0, 46 + texY, halfWidth, halfHeight);
        drawTexture(matrices, x, y + halfHeight, 0, 46 + texY + 14, halfWidth, halfHeight);

        drawTexture(matrices, x + halfWidth, y, 200 - halfWidth, 46 + texY, halfWidth, halfHeight);
        drawTexture(matrices, x + halfWidth, y + halfHeight, 200 - halfWidth, 46 + texY + 14, halfWidth, halfHeight);

        drawCenteredText(matrices, textRenderer, getMessage(), x + width / 2, (y + height / 2) - 4, active ? 16777215 : 10526880 | MathHelper.ceil(alpha * 255.0F) << 24);
    }
}
